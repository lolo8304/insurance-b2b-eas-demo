package com.sidis.eas;

import com.sidis.eas.flows.PatientRecordCreateFlow;
import com.sidis.eas.states.PatientRecordState;
import com.sidis.eas.states.StateVerifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PatientRecordFlowsFlowTests extends SidisBaseFlowTests {

    @Before
    public void setup() {
        this.setup(true, PatientRecordCreateFlow.Responder.class);
    }

    public static String dataJSONString() {
        return "{ \"person\": { \"firstName\": \"John\", \"lastName\": \"Doe\", \"dateOfBirth\": \"1993-09-13\", \"sex\": \"male\" }, \"address\": { \"street\": \"NE 29th Place\", \"city\": \"Bellevue\", \"zip\": \"14615\", \"state\": \"WA\", \"country\": \"USA\" }, \"communication\": { \"email\": \"john.doe@random.com\", \"phone\": \"(541) 754-3010\", \"mobile\": \"1-541-754-3010\" }, \"body-vitals\": { \"bloodType\": \"A+\", \"weight\": \"238 lb\", \"height\": \"6ft 2in\", \"bmi\": \"31.3\", \"bodyFat\": \"0.218\", \"muscleMass\": \"0.25\", \"hipSize\": \"33in\", \"bodyTemperature\": [98], \"heartRate\": [80], \"bloodPressure\": [130], \"respiratoryRate\": [27], \"sleepingBehaviour\": {}, \"pedometer/Day\": [6000] }, \"nutrition\": { \"foodAllergies\": [ \"egg\", \"nuts\" ], \"caloriesPerDay\": [2700], \"diets\": [], \"macroPerDay\": [700], \"microPerDay\": [200] }, \"allergies\": { \"types\": [ \"hayfever\", \"alergic asthma\" ] }, \"genetics\": { \"investigations\": [ \"geneticTest200610\", \"geneticTest151015\" ] }, \"medical-history\": {}, \"medication\": [ { \"drugName\": \"Aspirin\", \"isTakenPeriodically\": true } ], \"ongoingConditions\": [ \"Diabetes\" ], \"immunizations\": { \"types\": [ \"measles\", \"smallpox\" ] } }";
    }
    public static String dataJSONString_update() {
        return dataJSONString().replaceAll("John", "Johnny");
    }
    public static String dataJSONString_patch() {
        return "{ \"person\": { \"lastName\": \"Doering\" } }";
    }


    @Test
    public void create_record_ConstructedByFlowHasRightParties() throws Exception {
        SignedTransaction tx = this.newPatientDataCreateFlow(dataJSONString());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        PatientRecordState patientRecord = verifier
                .output().one()
                .one(PatientRecordState.class)
                .object();

        Assert.assertEquals("Swiss Life Ltd", insurer1Party, patientRecord.getPatient());
    }



    @Test
    public void update_record_ConstructedByFlowHasRightParties() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newPatientDataCreateFlow(dataJSONString()),
                this.ledgerServices);
        PatientRecordState patientRecord = verifier
                .output().one()
                .one(PatientRecordState.class)
                .object();

        StateVerifier verifier2 = StateVerifier.fromTransaction(
                this.newPatientDataUpdateFlow(dataJSONString_update()),
                this.ledgerServices);
        PatientRecordState patientRecord2 = verifier2
                .output().one()
                .one(PatientRecordState.class)
                .object();

        Assert.assertEquals("first name is John at beginning",
                "John",
                patientRecord.getDataValue("person.firstName"));
        Assert.assertEquals("first name is Johnny after update",
                "Johnny",
                patientRecord2.getDataValue("person.firstName"));
    }


    @Test
    public void patch_record_ConstructedByFlowHasRightParties() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newPatientDataCreateFlow(dataJSONString()),
                this.ledgerServices);
        PatientRecordState patientRecord = verifier
                .output().one()
                .one(PatientRecordState.class)
                .object();

        StateVerifier verifier2 = StateVerifier.fromTransaction(
                this.newPatientDataPatchFlow(dataJSONString_patch()),
                this.ledgerServices);
        PatientRecordState patientRecord2 = verifier2
                .output().one()
                .one(PatientRecordState.class)
                .object();

        Assert.assertEquals("first name is John at beginning",
                "John",
                patientRecord.getDataValue("person.firstName"));

        Assert.assertEquals("first name is still John because last Name updated only",
                "John",
                patientRecord2.getDataValue("person.firstName"));

        Assert.assertEquals("last name patched to Doering",
                "Doering",
                patientRecord2.getDataValue("person.lastName"));
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }


}