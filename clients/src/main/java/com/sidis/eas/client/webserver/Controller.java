package com.sidis.eas.client.webserver;

import com.sidis.eas.flows.ServiceFlow;
import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.ServiceState;
import com.sidis.eas.states.StateVerifier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static java.util.stream.Collectors.toList;


/**
 * Define your API endpoints here.
 */
@RestController
@CrossOrigin(origins = "http://localhost:63342")
@RequestMapping("/api/v1/") // The paths for HTTP requests are relative to this base path.
public class Controller {

    public static final boolean DEBUG = false;

    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    private final static String MAPPING_PATH = "/api/v1/";
    private final static String BASE_PATH = "sidis/eas";

    public Controller(NodeRPCConnection rpc) {
        ServiceState.State.values();
        ServiceState.StateTransition.values();
        if (DEBUG && rpc.proxy == null) {
            this.proxy = null;
            this.myLegalName = null;
            return;
        }
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    private ResponseEntity<List<StateAndLinks<ServiceState>>> getResponse(HttpServletRequest request, List<ServiceState> list, HttpStatus status) throws URISyntaxException {
        return new StateBuilder<>(list, ResponseEntity.status(status))
                .stateMapping(MAPPING_PATH, BASE_PATH, request)
                .links("services", x -> x.getState().getNextActions())
                .self("services")
                .buildList();
    }
    private ResponseEntity<StateAndLinks<ServiceState>> getResponse(HttpServletRequest request, ServiceState service, HttpStatus status) throws URISyntaxException {
        return new StateBuilder<>(service, ResponseEntity.status(HttpStatus.OK))
                .stateMapping(MAPPING_PATH, BASE_PATH, request)
                .self("services")
                .links("services", x -> x.getState().getNextActions())
                .build();
    }

    /**
     * Returns the node's name.
     */
    @GetMapping(value = BASE_PATH + "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = BASE_PATH + "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = proxy.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * returns all unconsumed services that exist in the node's vault.
     */
    @GetMapping(value = BASE_PATH + "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<StateAndLinks<ServiceState>>> getServices(
            HttpServletRequest request) throws URISyntaxException {
        List<ServiceState> list = proxy.vaultQuery(ServiceState.class).getStates()
                .stream().map(state -> state.getState().getData()).collect(toList());
        return this.getResponse(request, list, HttpStatus.OK);
    }

    /**
     * receives a unconsumed service with a given ID from the node's vault.
     * @param id unique identifier as UUID for mandate
     */
    @RequestMapping(
            value = BASE_PATH + "/services/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<StateAndLinks<ServiceState>> getUnconsumedServiceById(
            HttpServletRequest request,
            @PathVariable("id") String id) throws URISyntaxException {
        UniqueIdentifier uid = new UniqueIdentifier(null, UUID.fromString(id));
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Collections.singletonList(uid),
                Vault.StateStatus.UNCONSUMED,
                null);
        List<ServiceState> states = proxy.vaultQueryByCriteria(queryCriteria, ServiceState.class)
                .getStates().stream().map(state -> state.getState().getData()).collect(toList());
        if (states.isEmpty()) {
            return null;
        } else {
            ServiceState service = states.get(states.size()-1);
            return this.getResponse(request, service, HttpStatus.OK);
        }
    }

    /**
     * create a new patient record with given data
     * @param data string contains json patient data
     */
    @RequestMapping(
            value = BASE_PATH + "/services",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<StateAndLinks<ServiceState>> createService(
            HttpServletRequest request,
            @RequestParam(name = "service-name") String serviceName,
            @RequestParam(name = "data", required = false) String data,
            @RequestParam(name = "price", required = false) Integer price) {
        try {
            if (data == null || data.isEmpty() || JsonHelper.convertStringToJson(data) == null) {
                data = "{}";
            }
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(ServiceFlow.Create.class,
                            serviceName,
                            data,
                            price)
                    .getReturnValue()
                    .get();

            StateVerifier verifier = StateVerifier.fromTransaction(signedTx, null);
            ServiceState service = verifier.output().one(ServiceState.class).object();
            return this.getResponse(request, service, HttpStatus.CREATED);

        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new StateAndLinks<ServiceState>().error(ex));
        }
    }


    /**
     * execute an action on the services give by id
     * @param id identifier of the service
     * @param action name of action to be executed
     */
    @RequestMapping(
            value = BASE_PATH + "/services/{id}/{action}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<StateAndLinks<ServiceState>> serviceAction(
            HttpServletRequest request,
            @PathVariable("id") String id,
            @PathVariable("action") String action) {
        if (ServiceState.StateTransition.valueOf(action) == null) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(new StateAndLinks<ServiceState>().error("illegal action <"+action+">. Method not allowed"));
        }
        UniqueIdentifier uid = new UniqueIdentifier(null, UUID.fromString(id));
        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(ServiceFlow.Action.class,
                            uid,
                            action)
                    .getReturnValue()
                    .get();

            StateVerifier verifier = StateVerifier.fromTransaction(signedTx, null);
            ServiceState service = verifier.output().one(ServiceState.class).object();
            return this.getResponse(request, service, HttpStatus.OK);

        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new StateAndLinks<ServiceState>().error(ex));
        }
    }


}