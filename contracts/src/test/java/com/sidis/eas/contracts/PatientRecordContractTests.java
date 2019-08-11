package com.sidis.eas.contracts;

import com.sidis.eas.SidisBaseTests;
import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.PatientRecordState;
import net.corda.core.contracts.UniqueIdentifier;
import org.junit.Before;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class PatientRecordContractTests extends SidisBaseTests {

    @Before
    public void setup() {
        super.setup(false);
    }

    private String dataJSONString() {
        return "{ \"name\" : \"John Doe\" }";
    }

    private PatientRecordState newPatientRecord() {
        return new PatientRecordState(insurer1Party, new UniqueIdentifier(), this.dataJSONString(), JsonHelper.convertStringToJson(this.dataJSONString()));
    }


    @Test
    public void patient_create_normal() {
        transaction(ledgerServices, tx -> {
            PatientRecordState patientRecord = newPatientRecord();
            PatientRecordState patientRecord2 = newPatientRecord();
            tx.input(PatientRecordContract.ID, patientRecord2);
            tx.output(PatientRecordContract.ID, patientRecord);
            tx.command(patientRecord.getParticipantKeys(), new PatientRecordContract.Commands.Create());
            tx.failsWith("input must be empty");
            return null;
        });

        transaction(ledgerServices, tx -> {
            PatientRecordState patientRecord = newPatientRecord();
            tx.output(PatientRecordContract.ID, patientRecord);
            tx.command(patientRecord.getParticipantKeys(), new PatientRecordContract.Commands.Create());
            tx.verifies();
            return null;
        });
    }


}
