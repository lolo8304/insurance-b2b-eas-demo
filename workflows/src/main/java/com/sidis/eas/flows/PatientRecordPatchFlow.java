package com.sidis.eas.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sidis.eas.contracts.PatientRecordContract;
import com.sidis.eas.states.PatientRecordState;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


public class PatientRecordPatchFlow {

    @InitiatingFlow(version = 2)
    @StartableByRPC
    public static class Initiator extends BaseFlow {

        private String data;

        public Initiator(String data) {
            this.data = data;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return this.progressTracker_nosync_nocollect;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            getProgressTracker().setCurrentStep(PREPARATION);
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - search for our object by <id>
             * ===========================================================================*/
            StateAndRef<PatientRecordState> patientRecordRef = new FlowHelper<PatientRecordState>(this.getServiceHub()).getLastState(PatientRecordState.class);
            PatientRecordState patientRecord = this.getStateByRef(patientRecordRef);

            if (!me.equals(patientRecord.getPatient())) {
                throw new FlowException("Patient record can only be updated by patient.");
            }

            /* ============================================================================
             *      TODO 2 - Build our issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            getProgressTracker().setCurrentStep(BUILDING);
            PatientRecordState updatedPatientRecord = patientRecord
                    .updateValues(this.data);

            TransactionBuilder transactionBuilder = getTransactionBuilderSignedByParticipants(
                    patientRecord,
                    new PatientRecordContract.Commands.Update());
            transactionBuilder.addInputState(patientRecordRef);
            transactionBuilder.addOutputState(updatedPatientRecord, PatientRecordContract.ID);

            /* ============================================================================
             *          TODO 3 - Synchronize counterpart parties, send, sign and finalize!
             * ===========================================================================*/
            //return signCollectAndFinalize(me, updatedMandate.getWith(), transactionBuilder);
            return signAndFinalize(transactionBuilder);
        }

    }


    @InitiatedBy(Initiator.class)
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