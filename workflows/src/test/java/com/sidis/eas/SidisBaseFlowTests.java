package com.sidis.eas;

import com.sidis.eas.flows.PatientRecordCreateFlow;
import com.sidis.eas.flows.PatientRecordPatchFlow;
import com.sidis.eas.flows.PatientRecordUpdateFlow;
import com.sidis.eas.flows.ServiceFlow;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.concurrent.ExecutionException;

abstract public class SidisBaseFlowTests extends SidisBaseTests {


    protected SignedTransaction newPatientDataCreateFlow(String data) throws ExecutionException, InterruptedException {
        PatientRecordCreateFlow.Initiator flow = new PatientRecordCreateFlow.Initiator(data);
        CordaFuture<SignedTransaction> future = insurer1Node.startFlow(flow);
        network.runNetwork();
        return future.get();
    }
    protected SignedTransaction newPatientDataUpdateFlow(String data) throws ExecutionException, InterruptedException {
        PatientRecordUpdateFlow.Initiator flow = new PatientRecordUpdateFlow.Initiator(data);
        CordaFuture<SignedTransaction> future = insurer1Node.startFlow(flow);
        network.runNetwork();
        return future.get();
    }
    protected SignedTransaction newPatientDataPatchFlow(String data) throws ExecutionException, InterruptedException {
        PatientRecordPatchFlow.Initiator flow = new PatientRecordPatchFlow.Initiator(data);
        CordaFuture<SignedTransaction> future = insurer1Node.startFlow(flow);
        network.runNetwork();
        return future.get();
    }


    protected SignedTransaction newServiceCreateFlow(String serviceName, String data) throws ExecutionException, InterruptedException {
        FlowLogic<SignedTransaction> flow = new ServiceFlow.Create(serviceName, data);
        CordaFuture<SignedTransaction> future = insurer1Node.startFlow(flow);
        network.runNetwork();
        return future.get();
    }
    protected SignedTransaction newServiceShareFlow(UniqueIdentifier id, Party serviceProvider) throws ExecutionException, InterruptedException {
        FlowLogic<SignedTransaction> flow = new ServiceFlow.Share(id, serviceProvider);
        CordaFuture<SignedTransaction> future = insurer1Node.startFlow(flow);
        network.runNetwork();
        return future.get();
    }

}
