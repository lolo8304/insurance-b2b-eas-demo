package com.sidis.eas.contracts;

import com.sidis.eas.states.ServiceState;
import com.sidis.eas.states.StateVerifier;
import kotlin.Pair;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ServiceContract implements Contract {
    public static final String ID = "com.sidis.eas.contracts.ServiceContract";

    public ServiceContract() {
    }

    public interface Commands extends CommandData {
        public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException;

        class Common implements ServiceContract.Commands {

            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {

            }

            public Pair<ServiceState, ServiceState> verify1InOut(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                return requireThat(req -> {
                    ServiceState service1 = verifier.input().one().one(ServiceState.class).object();
                    ServiceState service2 = verifier
                            .output().notEmpty().one(ServiceState.class)
                            .object();
                    req.using("ID must be the same",
                            service1.getId().equals(service2.getId()));
                    req.using("client must be the same",
                            service1.getClient().equals(service2.getClient()));
                    return new Pair<>(service1, service2);
                });
            }

        }

        class Create extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    verifier.input().empty("input must be empty");
                    ServiceState service = verifier
                            .output().one().one(ServiceState.class)
                            .object();
                    req.using("service provider must be empty on creation",
                            service.getServiceProvider() == null);
                    req.using("state must be an initial state",
                            ServiceState.StateTransition.CREATE
                              .getInitialState()
                                  .equals(service.getState()));
                    return null;
                });
            }
        }
        class Update extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be the same",
                            service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                        ServiceState.StateTransition.UPDATE
                                .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    if (service1.getServiceProvider() == null) {
                            req.using("service provider must be both null",
                                    service2.getServiceProvider() == null);
                    } else {
                       req.using("service provider must be the same",
                                service1.getServiceProvider().equals(service2.getServiceProvider()));
                        req.using("service provider must be different than client",
                                !service2.getClient().equals(service2.getServiceProvider()));
                    }
                    return null;
                });
            }
        }
        class Share extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be different",
                            !service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                            ServiceState.StateTransition.SHARE
                                    .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    if (service1.getServiceProvider() != null) {
                        req.using("service provider must be the same",
                                service1.getServiceProvider().equals(service2.getServiceProvider()));
                    }
                    req.using("service provider must be provided",
                            service2.getServiceProvider() != null);
                    req.using("service provider must be different than client",
                            !service2.getClient().equals(service2.getServiceProvider()));
                    return null;
                });
            }
        }
        class Withdraw extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be different",
                            !service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                            ServiceState.StateTransition.WITHDRAW
                                    .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    if (service1.getServiceProvider() == null) {
                        req.using("service provider must be both null",
                                service2.getServiceProvider() == null);
                    } else {
                        req.using("service provider must be the same",
                                service1.getServiceProvider().equals(service2.getServiceProvider()));
                        req.using("service provider must be different than client",
                                !service2.getClient().equals(service2.getServiceProvider()));
                    }
                    return null;
                });
            }
        }
        class Accept extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be different",
                            !service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                            ServiceState.StateTransition.ACCEPT
                                    .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    req.using("service provider must be set",
                            service1.getServiceProvider() != null);
                    req.using("service provider must be set",
                            service2.getServiceProvider() != null);
                    req.using("service provider must be the same",
                            service1.getServiceProvider().equals(service2.getServiceProvider()));
                    req.using("service provider must be different than client",
                            !service2.getClient().equals(service2.getServiceProvider()));
                    return null;
                });
            }
        }
        class Cancel extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be different",
                            !service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                            ServiceState.StateTransition.CANCEL
                                    .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    req.using("service provider must be set",
                            service1.getServiceProvider() != null);
                    req.using("service provider must be set",
                            service2.getServiceProvider() != null);
                    req.using("service provider must be the same",
                            service1.getServiceProvider().equals(service2.getServiceProvider()));
                    req.using("service provider must be different than client",
                            !service2.getClient().equals(service2.getServiceProvider()));
                    return null;
                });

            }
        }
        class Decline extends Common implements ServiceContract.Commands {
            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                requireThat(req -> {
                    Pair<ServiceState, ServiceState> pair = verify1InOut(tx, verifier);
                    ServiceState service1 = pair.component1();
                    ServiceState service2 = pair.component2();
                    req.using("state must be different",
                            !service1.getState().equals(service2.getState()));
                    req.using("state <"+service2.getState()+"> is not valid next state from <"+service1.getState()+">",
                            ServiceState.StateTransition.DECLINE
                                    .getNextStateFrom(service1.getState())
                                    .equals(service2.getState()));
                    req.using("service provider must be set",
                            service1.getServiceProvider() != null);
                    req.using("service provider must be set",
                            service2.getServiceProvider() != null);
                    req.using("service provider must be the same",
                            service1.getServiceProvider().equals(service2.getServiceProvider()));
                    req.using("service provider must be different than client",
                            !service2.getClient().equals(service2.getServiceProvider()));
                    return null;
                });
            }
        }

        @CordaSerializable
        public class Reference extends ReferenceContract.Commands.Reference<ServiceState> implements ServiceContract.Commands {
            public Reference(ServiceState myState) {
                super(myState);
            }

            @Override
            public void verify(LedgerTransaction tx, StateVerifier verifier) throws IllegalArgumentException {
                this.verify(tx);
            }
        }
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        StateVerifier verifier = StateVerifier.fromTransaction(tx, ServiceContract.Commands.class);
        ServiceContract.Commands commandData = (ServiceContract.Commands)verifier.command();
        commandData.verify(tx, verifier);
    }

    private void verifyAllSigners(StateVerifier verifier) {
        requireThat(req -> {
            verifier
                    .output()
                    .participantsAreSigner("all participants must be signer");
            return null;
        });
    }


}
