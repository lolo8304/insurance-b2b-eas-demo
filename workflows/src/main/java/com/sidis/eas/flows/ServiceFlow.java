package com.sidis.eas.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sidis.eas.contracts.ServiceContract;
import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.ServiceState;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


public class ServiceFlow {

    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class Create extends BaseFlow {
        private final String serviceName;
        private final String data;

        public Create(String serviceName, String data) {
            this.serviceName = serviceName;
            this.data = data;
        }
        public Create(String serviceName) {
            this.serviceName = serviceName;
            this.data = "{}";
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            getProgressTracker().setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - Create our object !
             * ===========================================================================*/
            // We create our new TokenState.
            ServiceState serviceRecord = new ServiceState(
                    new UniqueIdentifier(),
                    this.serviceName,
                    me,
                    ServiceState.State.CREATED,
                    JsonHelper.convertStringToJson(this.data),
                    null, null);

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    serviceRecord,
                    new ServiceContract.Commands.Create());
            transactionBuilder.addOutputState(serviceRecord);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            return signAndFinalize(transactionBuilder);
        }

    }


    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class Share extends BaseFlow {
        private final UniqueIdentifier id;
        private final Party serviceProvider;

        public Share(UniqueIdentifier id, Party serviceProvider) {
            this.id = id;
            this.serviceProvider = serviceProvider;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            getProgressTracker().setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - Create our object !
             * ===========================================================================*/

            StateAndRef<ServiceState> serviceRef = new FlowHelper<ServiceState>(this.getServiceHub()).getLastStateByLinearId(ServiceState.class, this.id);
            if (serviceRef == null) {
                throw new FlowException("service with id "+this.id+" not found");
            }
            ServiceState service = this.getStateByRef(serviceRef);

            // We create our new TokenState.
            ServiceState sharedService = service.share(this.serviceProvider);

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    sharedService,
                    new ServiceContract.Commands.Share());
            transactionBuilder.addInputState(serviceRef);
            transactionBuilder.addOutputState(sharedService);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            return signCollectAndFinalize(me, this.serviceProvider, transactionBuilder);
        }

    }


    @InitiatedBy(ServiceFlow.Create.class)
    public static class CreateResponder extends ResponderBaseFlow<ServiceState> {

        public CreateResponder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }

    @InitiatedBy(ServiceFlow.Share.class)
    public static class ShareResponder extends ResponderBaseFlow<ServiceState> {

        public ShareResponder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }

}