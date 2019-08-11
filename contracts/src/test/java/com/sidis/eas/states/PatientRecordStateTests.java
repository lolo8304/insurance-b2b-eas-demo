package com.sidis.eas.states;

import com.sidis.eas.SidisBaseTests;
import net.corda.core.contracts.UniqueIdentifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PatientRecordStateTests extends SidisBaseTests {
    private final Instant expiryYearly = this.start.plus(1365, ChronoUnit.DAYS);
    private final Instant expiryMonthly = this.start.plus(6, ChronoUnit.DAYS);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        super.setup(false);
    }

    private String dataJSONString() {
        return "{ \"name\" : \"John Doe\", \"address\" : { \"street\" : \"NE 29th Place\"} }";
    }

    private PatientRecordState newSimplePatientCreator() {
        return new PatientRecordState(insurer1Party, new UniqueIdentifier(), this.dataJSONString(), JsonHelper.convertStringToJson(this.dataJSONString()));
    }

    @Test
    public void testSimplePatientCreator() {
        this.newSimplePatientCreator();
    }

    @Test
    public void testUpdatePatient() {

    }

    public PatientRecordState testUpdateToken(String token) {
        String dataUpdate = "{ \"wallet\": { \"token\": \""+token+"\" }}";
        PatientRecordState oldPatient = newSimplePatientCreator();
        return oldPatient.updateValues(dataUpdate);
    }

    @Test
    public void testUpdateNewToken() {
        PatientRecordState patient = testUpdateToken("abc");
        Assert.assertEquals("name should be John Doe", patient.getDataObject().get("name"), "John Doe");
        Assert.assertEquals("wallet token must be 'abc'",
                patient.getDataValue("wallet.token"),
                "abc");
    }
    @Test
    public void testUpdateExistingName() {
        PatientRecordState oldPatient = newSimplePatientCreator();
        PatientRecordState patient = oldPatient.updateValues("{\"name\" : \"Lolo\" }");
        Assert.assertEquals("name should be John Doe",
                patient.getDataValue("name"),
                "Lolo");
    }

    @Test
    public void testUpdateAddNewEntryInSubMap() {
        PatientRecordState oldPatient = newSimplePatientCreator();
        PatientRecordState patient = oldPatient.updateValues("{\"address\" : { \"city\" : \"Zurich\" }}");
        Assert.assertEquals("NE 29th Place should be new subkey",
                patient.getDataValue("address.city"),
                "Zurich");
        Assert.assertEquals("street should be still there",
                patient.getDataValue("address.street"),
                "NE 29th Place");
    }


}