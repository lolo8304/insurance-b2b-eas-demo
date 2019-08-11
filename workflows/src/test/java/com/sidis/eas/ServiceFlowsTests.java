package com.sidis.eas;

import com.sidis.eas.flows.ServiceFlow;
import com.sidis.eas.states.ServiceState;
import com.sidis.eas.states.StateVerifier;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceFlowsTests extends SidisBaseFlowTests {

    @Before
    public void setup() {
        this.setup(true,
                ServiceFlow.CreateResponder.class,
                ServiceFlow.ShareResponder.class);
    }

    public static String dataJSONString() {
        return "{ \"insurance-branch\" : \"health\", \"coverages\" : { \"OKP\" : true, \"ZVP\" : false } }";
    }
    public static String dataUpdateJSONString() {
        return "{ \"insurance-branch\" : \"health\", \"coverages\" : { \"OKP\" : true, \"ZVP\" : true, \"ADD-ON1\" : true } }";
    }
    public static String dataUpdateAfterShareJSONString() {
        return "{ \"insurance-branch\" : \"health\", \"coverages\" : { \"OKP\" : true, \"ZVP\" : true, \"ADD-ON1\" : true, \"UW\" : true } }";
    }


    @Test
    public void create_service() throws Exception {
        SignedTransaction tx = this.newServiceCreateFlow("Exit", dataJSONString());
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        ServiceState serviceRecord = verifier
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("OKP be true", "true", serviceRecord.getData("coverages.OKP"));
    }



    @Test
    public void share_service() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newServiceCreateFlow("Exit", dataJSONString()),
                this.ledgerServices);
        ServiceState service = verifier
                .output().one()
                .one(ServiceState.class)
                .object();

        StateVerifier verifier2 = StateVerifier.fromTransaction(
                this.newServiceShareFlow(service.getId(), this.insurer2Party),
                this.ledgerServices);
        ServiceState sharedService = verifier2
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("ZVP be false", "false", sharedService.getData("coverages.ZVP"));
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }


}