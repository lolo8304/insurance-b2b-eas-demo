package com.sidis.eas.states;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sidis.eas.contracts.ServiceContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

@BelongsToContract(ServiceContract.class)
public class ServiceState implements LinearState {

    @CordaSerializable
    public enum StateType {
        INITIAL,
        CONDITIONAL,
        FINAL
    }

    @CordaSerializable
    public enum State {
        CREATED(StateType.INITIAL),
        REGISTERED,
        INFORMED,
        CONFIRMED,

        TIMEOUTS(StateType.FINAL),
        WITHDRAWN(StateType.FINAL),

        SHARED,
        NOT_SHARED(StateType.FINAL),
        DUPLICATE(StateType.FINAL),

        PAYMENT_SENT,

        ACCEPTED(StateType.FINAL),
        DECLINED(StateType.FINAL);

        @JsonIgnore
        private StateType type;
        State(StateType type) {
            this.type = type;
        }
        State() {
            this(StateType.CONDITIONAL);
        }
        public StateType getType() { return this.type; }
        @JsonIgnore
        public boolean isFinalState() { return this.type == StateType.FINAL; }

    }

    @CordaSerializable
    public enum StateTransition {
        CREATE(State.CREATED),

        REGISTER(State.REGISTERED, State.CREATED),
        INFORM(State.INFORMED,     State.CREATED, State.REGISTERED),
        CONFIRM(State.CONFIRMED,   State.INFORMED),
        TIMEOUT(State.TIMEOUTS,    State.INFORMED),

        UPDATE(null,      State.CREATED, State.SHARED),

        SHARE(State.SHARED,         State.CONFIRMED, State.INFORMED, State.REGISTERED, State.CREATED),
        NO_SHARE(State.NOT_SHARED,  State.CONFIRMED, State.INFORMED, State.REGISTERED, State.CREATED),
        DUPLICATE(State.DUPLICATE, State.CONFIRMED, State.INFORMED, State.REGISTERED, State.CREATED),

        WITHDRAW(State.WITHDRAWN,   State.CREATED, State.REGISTERED, State.INFORMED, State.CONFIRMED, State.SHARED),

        SEND_PAYMENT(State.PAYMENT_SENT, State.SHARED),

        ACCEPT(State.ACCEPTED,      State.SHARED, State.PAYMENT_SENT),
        DECLINE(State.DECLINED,     State.SHARED, State.PAYMENT_SENT);

        @JsonIgnore
        private State nextState;
        @JsonIgnore
        private State[] currentStates;
        StateTransition(State nextState, @NotNull State... currentStates) {
            Arrays.sort(currentStates);
            this.currentStates = currentStates;
            this.nextState = nextState;
        }
        @JsonIgnore
        public boolean willBeInFinalState() {
            return this.nextState.isFinalState();
        }
        public State getNextStateFrom(State from) throws IllegalStateException {
            if (from.isFinalState()) {
                throw new IllegalStateException("state <"+from+"> is final state and cannot be transitioned");
            }
            if (Arrays.binarySearch(this.currentStates, from) >= 0) {
                return this.nextState != null ? this.nextState : from;
            }
            throw new IllegalStateException("state <"+from+"> is not allowed in this current transition");
        }
        public State getInitialState() throws IllegalStateException {
            if (this.currentStates.length == 0) {
                return this.nextState;
            }
            throw new IllegalStateException("transition has preconditions and is not an initial state");
        }
    }


    @NotNull
    private final UniqueIdentifier id;
    @NotNull
    private final State state;
    @NotNull
    private final String serviceName;
    @JsonIgnore
    @NotNull
    private final Party initiator;


    @NotNull
    private final Map<String, Object> serviceData;
    @JsonIgnore
    private final Party serviceProvider;
    private final Integer price;

    @ConstructorForDeserialization
    public ServiceState(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party initiator, @NotNull State state, Map<String, Object> serviceData, Party serviceProvider, Integer price) {
        this.id = id;
        this.state = state;
        this.serviceName = serviceName;
        this.initiator = initiator;
        this.serviceData = serviceData == null ? new LinkedHashMap<>() : serviceData;
        this.serviceProvider = serviceProvider;
        this.price = price;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.id;
    }

    @NotNull
    @JsonIgnore
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> list = new ArrayList<>();
        list.add(this.initiator);
        if (this.serviceProvider != null) list.add(this.serviceProvider);
        return list;
    }

    @NotNull
    @JsonIgnore
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @NotNull
    public UniqueIdentifier getId() {
        return id;
    }
    @NotNull
    public String getServiceName() {
        return serviceName;
    }
    @NotNull
    public Party getInitiator() {
        return initiator;
    }
    @NotNull
    public State getState() { return state; }
    public Party getServiceProvider() {
        return serviceProvider;
    }


    @NotNull
    public String getInitiatorX500() {
        return initiator.getName().getX500Principal().getName();
    }
    public String getServiceProviderX500() {
        return serviceProvider != null ? serviceProvider.getName().getX500Principal().getName() : "";
    }
    public Integer getPrice() {
        return price;
    }
    public Map<String, Object> getServiceData() {
        return serviceData;
    }


    /* actions CREATE */
    public static ServiceState create(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party initiator, Map<String, Object> serviceData) {
        return new ServiceState(id, serviceName, initiator, StateTransition.CREATE.getInitialState(), serviceData, null, null);
    }
    /* actions UPDATE */
    public ServiceState update(Map<String, Object> newServiceData) {
        return this.update(newServiceData, this.price);
    }
    public ServiceState update(Map<String, Object> newServiceData, Integer newPrice) {
        State newState = StateTransition.UPDATE.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.initiator, newState, newServiceData, this.serviceProvider, newPrice);
    }

    /* actions SHARE */
    public ServiceState share(@NotNull Party newServiceProvider) {
        State newState = StateTransition.SHARE.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.initiator, newState, this.serviceData, newServiceProvider, this.price);
    }


    /* actions any */
    public ServiceState withAction(StateTransition transition) {
        State newState = transition.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.initiator, newState, this.serviceData, this.serviceProvider, this.price);
    }

}
