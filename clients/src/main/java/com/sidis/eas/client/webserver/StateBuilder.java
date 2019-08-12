package com.sidis.eas.client.webserver;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

public class StateBuilder {

    private final LinearState state;
    private final BodyBuilder builder;
    private RestHelper apiHelper;

    public StateBuilder(LinearState state, BodyBuilder builder) {
        this.state = state;
        this.builder = builder;
    }

    private class RestHelper {
        private final String mappingPath;
        private final String bashPath;
        private final HttpServletRequest request;
        private final Map<String, URI> links = new LinkedHashMap<>();
        private URI linkSelf;

        public RestHelper(String mappingPath, String basePath, HttpServletRequest request) {
            this.mappingPath = mappingPath;
            this.bashPath = basePath;
            this.request = request;
        }

        protected URI getRoot() throws URISyntaxException {
            return new URI(request.getScheme(), null, request.getServerName(), request.getServerPort(), null, null, null);
        }
        protected URI createURI(String subpath) throws URISyntaxException {
            return this.getRoot().resolve(this.mappingPath+this.bashPath+"/"+subpath);
        }
        public void self(String modelPlural, UniqueIdentifier id) throws URISyntaxException {
            this.linkSelf = this.createURI(modelPlural + "/" + id.getId().toString());
        }

        public void link(String modelPlural, UniqueIdentifier id, String action) throws URISyntaxException {
            this.links.put(
                    action,
                    createURI(modelPlural + "/" + id.getId().toString() + "/" + action)
            );
        }
    }

    public StateBuilder stateMapping(String mappingPath, String basePath, HttpServletRequest request) {
        this.apiHelper = new RestHelper(mappingPath, basePath, request);
        return this;
    }

    public StateBuilder link(String modelPlural, String action) throws URISyntaxException {
        if (apiHelper != null) {
            apiHelper.link(modelPlural, this.state.getLinearId(), action);
        }
        return this;
    }
    public StateBuilder link(String modelPlural, String[] actions) throws URISyntaxException {
        if (apiHelper != null) {
            for (int i = 0; i < actions.length ; i++) {
                apiHelper.link(modelPlural, this.state.getLinearId(), actions[i]);
            }
        }
        return this;
    }
    public StateBuilder self(String modelPlural) throws URISyntaxException {
        if (apiHelper != null) {
            apiHelper.self(modelPlural, this.state.getLinearId());
        }
        return this;
    }

    public ResponseEntity<StateAndLinks> build() {
        StateAndLinks body = new StateAndLinks(this.state);
        if (apiHelper != null) {
            if (apiHelper.linkSelf != null) {
                this.builder.location(apiHelper.linkSelf);
                body.self(apiHelper.linkSelf);
            }
            body.links(apiHelper.links);
        }
        return this.builder.body(body);
    }
}
