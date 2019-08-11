package com.sidis.eas.contracts;

import com.sidis.eas.SidisBaseTests;
import com.sidis.eas.states.JsonHelper;
import com.sidis.eas.states.ServiceState;
import com.sidis.eas.states.ServiceStateTests;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ServiceContractTests extends SidisBaseTests {

    @Before
    public void setup() {
        super.setup(false);
    }

    private ServiceState newService() {
        return ServiceState.create(
                new UniqueIdentifier(), "insurance", insurer1Party,
                JsonHelper.convertStringToJson(ServiceStateTests.dataJSONString()));
    }
    private ServiceState updateService(ServiceState service) {
        return service.update(JsonHelper.convertStringToJson(ServiceStateTests.dataUpdateJSONString()));
    }
    private ServiceState shareService(ServiceState service, @NotNull Party serviceProvider) {
        return service.share(serviceProvider);
    }
    private ServiceState withdraw(ServiceState service) {
        return service.withdraw();
    }
    private ServiceState accept(ServiceState service) {
        return service.accept();
    }
    private ServiceState decline(ServiceState service) {
        return service.decline();
    }
    private ServiceState cancel(ServiceState service) {
        return service.cancel();
    }
    private ServiceState updateAfterShareService(ServiceState service) {
        return service.update(JsonHelper.convertStringToJson(ServiceStateTests.dataUpdateAfterShareJSONString()));
    }
    private ServiceState setInvalidState(ServiceState serviceState, ServiceState.State newState) {
        return new ServiceState(
                serviceState.getId(),
                serviceState.getServiceName(),
                serviceState.getClient(),
                newState,
                serviceState.getServiceData(),
                serviceState.getServiceProvider(),
                serviceState.getPrice());
    }
    private ServiceState setInvalidStateProvider(ServiceState serviceState, ServiceState.State newState, Party newProvider) {
        return new ServiceState(
                serviceState.getId(),
                serviceState.getServiceName(),
                serviceState.getClient(),
                newState,
                serviceState.getServiceData(),
                newProvider,
                serviceState.getPrice());
    }


    @Test
    public void service_create_normal_no_initial_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = setInvalidState(service1, ServiceState.State.SHARED);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Create());
            tx.failsWith("state must be an initial state");
            return null;
        });
    }

    @Test
    public void service_create_normal_input_not_empty() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = newService();
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Create());
            tx.failsWith("input must be empty");
            return null;
        });
    }

    @Test
    public void service_create_normal_double_output() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = newService();
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Create());
            tx.failsWith("input must be empty");
            return null;
        });
    }

    @Test
    public void service_create_normal_double_input() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = newService();
            tx.output(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Create());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });

    }

    @Test
    public void service_create_normal() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            tx.output(ServiceContract.ID, service1);
            tx.command(service1.getParticipantKeys(), new ServiceContract.Commands.Create());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void service_update_normal_no_input() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = updateService(service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });

    }

    @Test
    public void service_update_normal_not_same_ID() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = updateService(newService());
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("ID must be the same");
            return null;
        });
    }

    @Test
    public void service_update_normal_ACCEPTED_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service1a = setInvalidState(service1, ServiceState.State.ACCEPTED);
            ServiceState service2 = setInvalidState(updateService(service1), ServiceState.State.ACCEPTED);
            tx.input(ServiceContract.ID, service1a);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("state <ACCEPTED> is not allowed in this current transition");
            return null;
        });

    }

    @Test
    public void service_update_normal_WITHDRAWN_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service1a = setInvalidState(service1, ServiceState.State.WITHDRAWN);
            ServiceState service2 = setInvalidState(updateService(service1), ServiceState.State.WITHDRAWN);
            tx.input(ServiceContract.ID, service1a);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("state <WITHDRAWN> is not allowed in this current transition");
            return null;
        });

    }

    @Test
    public void service_update_normal_CANCELED_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service1a = setInvalidState(service1, ServiceState.State.CANCELED);
            ServiceState service2 = setInvalidState(updateService(service1), ServiceState.State.CANCELED);
            tx.input(ServiceContract.ID, service1a);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("state <CANCELED> is not allowed in this current transition");
            return null;
        });
    }

    @Test
    public void service_update_normal_DECLINED_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service1a = setInvalidState(service1, ServiceState.State.DECLINED);
            ServiceState service2 = setInvalidState(updateService(service1), ServiceState.State.DECLINED);
            tx.input(ServiceContract.ID, service1a);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("state <DECLINED> is not allowed in this current transition");
            return null;
        });


    }

    @Test
    public void service_update_normal_double_output_same() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = updateService(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });
    }

    @Test
    public void service_update_normal_double_output_different() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = updateService(service1);
            ServiceState service3 = updateService(service2);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.output(ServiceContract.ID, service3);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });


    }

    @Test
    public void service_update_normal() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = updateService(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.verifies();
            return null;
        });

    }



    @Test
    public void service_share_direct_failed() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = shareService(service1, insurer2Party);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Share());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });
    }


    @Test
    public void service_share_direct_failed_same_parties() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = shareService(service1, insurer1Party);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Share());
            tx.failsWith("service provider must be different than client");
            return null;
        });
    }

    @Test
    public void service_share_direct_failed_sp_null() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = shareService(service1, null);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Share());
            tx.failsWith("service provider must be provided");
            return null;
        });
    }

    @Test
    public void service_share_direct() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = shareService(service1, insurer2Party);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Share());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void service_share_updated() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service1a = updateService(service1);
            ServiceState service2 = shareService(service1a, insurer2Party);
            tx.input(ServiceContract.ID, service1a);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Share());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void service_create_withdraw_failed_no_input() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = withdraw(service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Withdraw());
            tx.failsWith("List must contain only 1 entry");
            return null;
        });
    }

    @Test
    public void service_create_withdraw_wrong_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = withdraw(service1);
            ServiceState service2a = setInvalidState(service2, ServiceState.State.ACCEPTED);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2a);
            tx.command(service2a.getParticipantKeys(), new ServiceContract.Commands.Withdraw());
            tx.failsWith("Failed requirement: state <ACCEPTED> is not valid next state from <CREATED>");
            return null;
        });
    }


    @Test
    public void service_create_withdraw() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = withdraw(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Withdraw());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void service_create_withdraw_updated() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = updateService(newService());
            ServiceState service2 = withdraw(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Withdraw());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void service_create_withdraw_updated_shared() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(updateService(newService()), insurer2Party);
            ServiceState service2 = withdraw(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Withdraw());
            tx.verifies();
            return null;
        });
    }



    @Test
    public void service_update_shared_updated() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(updateService(newService()), insurer2Party);
            ServiceState service2 = updateService(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void service_update2_shared_updated() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = updateService(shareService(updateService(newService()), insurer2Party));
            ServiceState service2 = updateService(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Update());
            tx.verifies();
            return null;
        });
    }



    @Test
    public void service_decline() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(newService(), insurer2Party);
            ServiceState service2 = decline(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Decline());
            tx.verifies();
            return null;
        });
    }



    @Test
    public void service_decline_after_update() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = updateService(shareService(newService(), insurer2Party));
            ServiceState service2 = decline(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Decline());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void service_decline_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(newService(), insurer2Party);
            ServiceState service2 = setInvalidState(decline(service1), ServiceState.State.CANCELED);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Decline());
            tx.failsWith("state <CANCELED> is not valid next state from <SHARED>");
            return null;
        });
    }


    @Test(expected = IllegalStateException.class)
    public void service_decline_invalid_pre_state_on_state_feature() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = decline(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Decline());
            tx.failsWith("any error");
            return null;
        });
    }


    @Test
    public void service_decline_invalid_pre_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = setInvalidStateProvider(service1, ServiceState.State.DECLINED, insurer2Party);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Decline());
            tx.failsWith("state <CREATED> is not allowed in this current transition");
            return null;
        });
    }



    @Test
    public void service_accept() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(newService(), insurer2Party);
            ServiceState service2 = accept(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Accept());
            tx.verifies();
            return null;
        });
    }



    @Test
    public void service_accept_after_update() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = updateService(shareService(newService(), insurer2Party));
            ServiceState service2 = accept(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Accept());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void service_accept_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(newService(), insurer2Party);
            ServiceState service2 = setInvalidState(accept(service1), ServiceState.State.CANCELED);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Accept());
            tx.failsWith("state <CANCELED> is not valid next state from <SHARED>");
            return null;
        });
    }


    @Test(expected = IllegalStateException.class)
    public void service_accept_invalid_pre_state_on_state_feature() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = accept(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Accept());
            tx.failsWith("any error");
            return null;
        });
    }


    @Test
    public void service_accept_invalid_pre_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = newService();
            ServiceState service2 = setInvalidStateProvider(service1, ServiceState.State.ACCEPTED, insurer2Party);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Accept());
            tx.failsWith("state <CREATED> is not allowed in this current transition");
            return null;
        });
    }



    @Test
    public void service_cancel() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = accept(shareService(updateService(newService()), insurer2Party));
            ServiceState service2 = cancel(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Cancel());
            tx.verifies();
            return null;
        });
    }

    @Test(expected = IllegalStateException.class)
    public void service_cancel_invalid_state() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = shareService(updateService(newService()), insurer2Party);
            ServiceState service2 = cancel(service1);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Cancel());
            tx.verifies();
            return null;
        });
    }


    @Test
    public void service_cancel_invalid_state_illegal_set() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = accept(shareService(updateService(newService()), insurer2Party));
            ServiceState service2 = setInvalidState(service1, ServiceState.State.DECLINED);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Cancel());
            tx.failsWith("state <DECLINED> is not valid next state from <ACCEPTED>");
            return null;
        });
    }


    @Test
    public void service_cancel_wrong_provider() {
        transaction(ledgerServices, tx -> {
            ServiceState service1 = accept(shareService(updateService(newService()), insurer2Party));
            ServiceState service2 = setInvalidStateProvider(service1, ServiceState.State.CANCELED, fzl1Party);
            tx.input(ServiceContract.ID, service1);
            tx.output(ServiceContract.ID, service2);
            tx.command(service2.getParticipantKeys(), new ServiceContract.Commands.Cancel());
            tx.failsWith("service provider must be the same");
            return null;
        });
    }


}
