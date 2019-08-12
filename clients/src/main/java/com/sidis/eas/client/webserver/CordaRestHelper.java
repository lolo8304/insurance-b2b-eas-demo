package com.sidis.eas.client.webserver;

import net.corda.core.contracts.UniqueIdentifier;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class CordaRestHelper {
    private final String mappingPath;
    private final String bashPath;
    private final HttpServletRequest request;
    private final List<String> links = new ArrayList<>();
    private URI linkSelf;

    public CordaRestHelper(String mappingPath, String basePath, HttpServletRequest request) {
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
        this.links.add(
                createURI(modelPlural + "/" + id.getId().toString() + "/" + action).toString() + ";" + action);
    }

    public ResponseEntity.BodyBuilder buildLinks(ResponseEntity.BodyBuilder builder) {
        if (linkSelf != null) builder = builder.location(this.linkSelf);
        for (String link: this.links) {
            builder = builder.header("Link", link);
        }
        return builder;
    }

}