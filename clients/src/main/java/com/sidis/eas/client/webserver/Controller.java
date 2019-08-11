package com.sidis.eas.client.webserver;

import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.StateVerifier;
import com.sidis.eas.flows.PatientRecordCreateFlow;
import com.sidis.eas.flows.PatientRecordPatchFlow;
import com.sidis.eas.flows.PatientRecordUpdateFlow;
import com.sidis.eas.states.PatientRecordState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Define your API endpoints here.
 */
@RestController
@CrossOrigin(origins = "http://localhost:63342")
@RequestMapping("/api/v1/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    private final static String BASE_PATH = "sidis/eas";

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();
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
     * returns the patient records that exist in the node's vault.
     */
    @GetMapping(value = BASE_PATH + "/patient-records", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PatientRecordState> getPatientRecords() {
        List<PatientRecordState> states = proxy.vaultQuery(PatientRecordState.class).getStates()
                .stream().map(state -> state.getState().getData()).collect(toList());
        return states;
    }

    /**
     * receives a mandate that exist with a given ID from the node's vault.
     * @param id unique identifier as UUID for mandate
     */
    @RequestMapping(
            value = BASE_PATH + "/patient-records/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public PatientRecordState getPatientRecord(@PathVariable("id") String id) {
        UniqueIdentifier uid = new UniqueIdentifier(null, UUID.fromString(id));
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                Arrays.asList(uid),
                Vault.StateStatus.ALL,
                null);
        List<PatientRecordState> states = proxy.vaultQueryByCriteria(queryCriteria, PatientRecordState.class)
                .getStates().stream().map(state -> state.getState().getData()).collect(toList());
        return states.isEmpty() ? null : states.get(states.size()-1);
    }

    /**
     * create a new patient record with given data
     * @param data string contains json patient data
     */
    @RequestMapping(
            value = BASE_PATH + "/patient-records",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PatientRecordState> createPatientRecord(@RequestParam String data) {
        try {
            if (data == null || JsonHelper.convertStringToJson(data) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(PatientRecordCreateFlow.Initiator.class,
                            data)
                    .getReturnValue()
                    .get();

            StateVerifier verifier = StateVerifier.fromTransaction(signedTx, null);
            PatientRecordState patientRecord = verifier.output().one(PatientRecordState.class).object();
            /*
            URI acceptLink = this.link("mandates", mandate.getId(), "accept");
            URI denyLink = this.link("mandates", mandate.getId(), "deny");
            URI withdrawLink = this.link("mandates", mandate.getId(), "withdraw");
            */
            return ResponseEntity.status(HttpStatus.CREATED).body(patientRecord);

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }
    }

    /**
     * udpate an existing patient record with given data
     * @param data string contains json patient data
     */
    @RequestMapping(
            value = BASE_PATH + "/patient-records",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PatientRecordState> updatePatientRecord(@RequestParam String data) {
        try {
            if (data == null || JsonHelper.convertStringToJson(data) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(PatientRecordUpdateFlow.Initiator.class,
                            data)
                    .getReturnValue()
                    .get();

            StateVerifier verifier = StateVerifier.fromTransaction(signedTx, null);
            PatientRecordState patientRecordOutput = verifier.output().one(PatientRecordState.class).object();
            /*
            URI acceptLink = this.link("mandates", mandate.getId(), "accept");
            URI denyLink = this.link("mandates", mandate.getId(), "deny");
            URI withdrawLink = this.link("mandates", mandate.getId(), "withdraw");
            */
            return ResponseEntity.status(HttpStatus.OK).body(patientRecordOutput);

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }
    }


    /**
     * patch / append the patient record with given data
     * @param data string contains json patient data to be added / updated
     */
    @RequestMapping(
            value = BASE_PATH + "/patient-records",
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PatientRecordState> patchPatientRecord(@RequestParam String data) {
        try {
            if (data == null || JsonHelper.convertStringToJson(data) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (IllegalStateException ex) {
            logger.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(PatientRecordPatchFlow.Initiator.class,
                            data)
                    .getReturnValue()
                    .get();

            StateVerifier verifier = StateVerifier.fromTransaction(signedTx, null);
            PatientRecordState patientRecordOutput = verifier.output().one(PatientRecordState.class).object();
            /*
            URI acceptLink = this.link("mandates", mandate.getId(), "accept");
            URI denyLink = this.link("mandates", mandate.getId(), "deny");
            URI withdrawLink = this.link("mandates", mandate.getId(), "withdraw");
            */
            return ResponseEntity.status(HttpStatus.OK).body(patientRecordOutput);

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }
    }


}