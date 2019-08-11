package com.sidis.eas.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sidis.eas.contracts.PatientRecordContract;
import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.PatientRecordState;
import kotlin.Unit;
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


public class PatientRecordCreateFlow {

    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class Initiator extends BaseFlow {
        private final String data;

        public Initiator(String data) {
            this.data = data;
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
            PatientRecordState patientRecord = new PatientRecordState(me, new UniqueIdentifier(), data, JsonHelper.convertStringToJson(data));

            /* ============================================================================
             *      TODO 3 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    patientRecord,
                    new PatientRecordContract.Commands.Create());
            transactionBuilder.addOutputState(patientRecord,
                    PatientRecordContract.ID, AlwaysAcceptAttachmentConstraint.INSTANCE);

            /* ============================================================================
             *          TODO 2 - Write our contract to control issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            return signAndFinalize(transactionBuilder);
        }

    }
    @InitiatedBy(PatientRecordCreateFlow.Initiator.class)
    public static class Responder extends ResponderBaseFlow<PatientRecordState> {

        public Responder(FlowSession otherFlow) {
            super(otherFlow);
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            return this.receiveCounterpartiesNoTxChecking();
        }
    }
}