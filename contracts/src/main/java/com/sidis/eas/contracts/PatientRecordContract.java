package com.sidis.eas.contracts;

import com.sidis.eas.states.PatientRecordState;
import com.sidis.eas.states.StateVerifier;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class PatientRecordContract implements Contract {
    public static final String ID = "com.sidis.eas.contracts.PatientRecordContract";

    public PatientRecordContract() {
    }

    public interface Commands extends CommandData {
        class Create implements PatientRecordContract.Commands { }
        class Update implements PatientRecordContract.Commands { }
        class Patch implements PatientRecordContract.Commands { }

        @CordaSerializable
        public class Reference extends ReferenceContract.Commands.Reference<PatientRecordState> implements PatientRecordContract.Commands {
            public Reference(PatientRecordState myState) {
                super(myState);
            }
        }
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        StateVerifier verifier = StateVerifier.fromTransaction(tx, PatientRecordContract.Commands.class);
        CommandData commandData = verifier.command();
        if (commandData instanceof PatientRecordContract.Commands.Create) {
            verifyCreate(tx, verifier);
        } else if (commandData instanceof PatientRecordContract.Commands.Update) {
            verifyUpdate(tx, verifier);
        } else if (commandData instanceof PatientRecordContract.Commands.Patch) {
            verifyPatch(tx, verifier);
        }
    }

    private void verifyCreate(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            verifier.input().empty("input must be empty");
            PatientRecordState patient = verifier
                    .output().one().one(PatientRecordState.class)
                    .object();
            this.verifyAllSigners(verifier);
            return null;
        });
    }

    private void verifyAllSigners(StateVerifier verifier) {
        requireThat(req -> {
            verifier
                    .output()
                    .participantsAreSigner("all participants must be signer");
            return null;
        });
    }


    private void verifyUpdate(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            PatientRecordState input = verifier.input().one().one(PatientRecordState.class).object();
            PatientRecordState output = verifier.output().one().one(PatientRecordState.class).object();
            req.using(
                    "ID must be same",
                    input.getId().equals(output.getId()));
            req.using(
                    "patient must be same",
                    input.getPatient().equals(output.getPatient()));
            req.using(
                    "map should not be the same",
                    !input.getDataObject().equals(output.getDataObject()));
            this.verifyAllSigners(verifier);
            return null;
        });
    }

    private void verifyPatch(LedgerTransaction tx, StateVerifier verifier) {
        requireThat(req -> {
            PatientRecordState input = verifier.input().one().one(PatientRecordState.class).object();
            PatientRecordState output = verifier.output().one().one(PatientRecordState.class).object();
            req.using(
                    "patient must be same",
                    input.getId().equals(output.getId()));
            req.using(
                    "patient must be same",
                    input.getPatient().equals(output.getPatient()));
            req.using(
                    "map should not be the same",
                    !input.getDataObject().equals(output.getDataObject()));
            this.verifyAllSigners(verifier);
            return null;
        });
    }

}
