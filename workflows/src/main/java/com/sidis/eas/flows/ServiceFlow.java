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
        private final Integer price;

        public Create(String serviceName, String data, Integer price) {
            this.serviceName = serviceName;
            this.data = data;
            this.price = price;
        }
        public Create(String serviceName) {
            this.serviceName = serviceName;
            this.data = "{}";
            this.price = 0;
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
                    null, this.price);

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
    public static class Update extends BaseFlow {
        private final UniqueIdentifier id;
        private final String data;
        private final Integer price;

        public Update(UniqueIdentifier id, String data, Integer price) {
            this.id = id;
            this.data = data;
            this.price = price;
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
            ServiceState sharedService = service.update(
                    JsonHelper.convertStringToJson(this.data),
                    this.price
            );

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    sharedService,
                    new ServiceContract.Commands.Update());
            transactionBuilder.addInputState(serviceRef);
            transactionBuilder.addOutputState(sharedService);

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



    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class ActionAfterShare extends BaseFlow {
        private final UniqueIdentifier id;
        private final String action;

        public ActionAfterShare(UniqueIdentifier id, String action) {
            this.id = id;
            this.action = action;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync;
        }

        private ServiceState.StateTransition getTransition() {
            return ServiceState.StateTransition.valueOf(this.action);
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
            ServiceState sharedService = service.withAction(this.getTransition());

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    sharedService,
                    new ServiceContract.Commands.ActionAfterShare(this.action));
            transactionBuilder.addInputState(serviceRef);
            transactionBuilder.addOutputState(sharedService);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            if (sharedService.getServiceProvider() == null) {
                return signAndFinalize(transactionBuilder);
            } else {
                return signCollectAndFinalize(me, sharedService.getServiceProvider(), transactionBuilder);
            }
        }

    }


    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class ActionBeforeShare extends BaseFlow {
        private final UniqueIdentifier id;
        private final String action;

        public ActionBeforeShare(UniqueIdentifier id, String action) {
            this.id = id;
            this.action = action;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync;
        }

        private ServiceState.StateTransition getTransition() {
            return ServiceState.StateTransition.valueOf(this.action);
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
            ServiceState newService = service.withAction(this.getTransition());

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    newService,
                    new ServiceContract.Commands.ActionBeforeShare(this.action));
            transactionBuilder.addInputState(serviceRef);
            transactionBuilder.addOutputState(newService);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            if (newService.getServiceProvider() == null) {
                return signAndFinalize(transactionBuilder);
            } else {
                return signCollectAndFinalize(me, newService.getServiceProvider(), transactionBuilder);
            }
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

    @InitiatedBy(ServiceFlow.Update.class)
    public static class UpdateResponder extends ResponderBaseFlow<ServiceState> {

        public UpdateResponder(FlowSession otherFlow) {
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
    @InitiatedBy(ActionAfterShare.class)
    public static class ActionAfterShareResponder extends ResponderBaseFlow<ServiceState> {

        public ActionAfterShareResponder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }

    @InitiatedBy(ActionBeforeShare.class)
    public static class ActionBeforeShareResponder extends ResponderBaseFlow<ServiceState> {

        public ActionBeforeShareResponder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }
}