package com.sidis.eas;

import com.sidis.eas.flows.ServiceFlow;
import com.sidis.eas.states.ServiceState;
import com.sidis.eas.states.StateVerifier;
import net.corda.core.transactions.SignedTransaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceFlowTests extends SidisBaseFlowTests {

    @Before
    public void setup() {
        this.setup(true,
            ServiceFlow.CreateResponder.class,
            ServiceFlow.UpdateResponder.class,
            ServiceFlow.ShareResponder.class,
            ServiceFlow.ActionAfterShareResponder.class,
            ServiceFlow.ActionBeforeShareResponder.class
        );
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
        SignedTransaction tx = this.newServiceCreateFlow("Exit", dataJSONString(), 7);
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        ServiceState service = verifier
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("OKP be true", "true", service.getData("coverages.OKP"));
        Assert.assertEquals("price must be 42", "7", String.valueOf(service.getPrice()));
    }


    @Test
    public void update_before_share_service() throws Exception {
        SignedTransaction tx = this.newServiceCreateFlow("Exit", dataJSONString(), 7);
        StateVerifier verifier = StateVerifier.fromTransaction(tx, this.ledgerServices);
        ServiceState service = verifier
                .output().one()
                .one(ServiceState.class)
                .object();
        Assert.assertEquals("ZVP must be false", "false", service.getData("coverages.ZVP"));

        StateVerifier verifier2 = StateVerifier.fromTransaction(
                this.newServiceUpdateFlow(service.getId(), dataUpdateJSONString(), 42),
                this.ledgerServices);
        ServiceState service2 = verifier2
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("ZVP must be true", "true", service2.getData("coverages.ZVP"));
        Assert.assertEquals("price must be 42", "42", String.valueOf(service2.getPrice()));
    }



    @Test
    public void share_service() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newServiceCreateFlow("Exit", dataJSONString(), 7),
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


    @Test
    public void action_ACCEPT_service() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newServiceCreateFlow("Exit", dataJSONString(), 7),
                this.ledgerServices);
        ServiceState service = verifier
                .output().one()
                .one(ServiceState.class)
                .object();

        StateVerifier verifierS = StateVerifier.fromTransaction(
                this.newServiceShareFlow(service.getId(), insurer2Party),
                this.ledgerServices);
        ServiceState serviceS = verifierS
                .output().one()
                .one(ServiceState.class)
                .object();
        Assert.assertEquals("state is SHARED", "SHARED", serviceS.getState().toString());

        StateVerifier verifierA = StateVerifier.fromTransaction(
                this.newServiceActionAfterShareFlow(serviceS.getId(), "ACCEPT"),
                this.ledgerServices);
        ServiceState serviceA = verifierA
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("ZVP be false", "false", serviceA.getData("coverages.ZVP"));
        Assert.assertEquals("insurer2 must be service provider", insurer2Party, serviceA.getServiceProvider());
        Assert.assertEquals("state is ACCEPTED", "ACCEPTED", serviceA.getState().toString());
    }


    @Test
    public void action_CONFIRM_service() throws Exception {
        StateVerifier verifier = StateVerifier.fromTransaction(
                this.newServiceCreateFlow("Exit", dataJSONString(), 7),
                this.ledgerServices);
        ServiceState service = verifier
                .output().one()
                .one(ServiceState.class)
                .object();

        StateVerifier verifier1 = StateVerifier.fromTransaction(
                this.newServiceActionBeforeShareFlow(service.getId(), "INFORM"),
                this.ledgerServices);
        ServiceState service1 = verifier1
                .output().one()
                .one(ServiceState.class)
                .object();

        StateVerifier verifier2 = StateVerifier.fromTransaction(
                this.newServiceActionBeforeShareFlow(service1.getId(), "CONFIRM"),
                this.ledgerServices);
        ServiceState service2 = verifier2
                .output().one()
                .one(ServiceState.class)
                .object();

        Assert.assertEquals("ZVP be false", "false", service2.getData("coverages.ZVP"));
        Assert.assertEquals("state is CONFIRMED", "CONFIRMED", service2.getState().toString());
    }


    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }


}