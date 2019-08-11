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
        WITHDRAWN(StateType.FINAL),
        SHARED(StateType.INITIAL),
        ACCEPTED,
        DECLINED(StateType.FINAL),
        CANCELED(StateType.FINAL);

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
        UPDATE(null, State.CREATED, State.SHARED),
        SHARE(State.SHARED, State.CREATED),
        WITHDRAW(State.WITHDRAWN, State.CREATED, State.SHARED),
        ACCEPT(State.ACCEPTED, State.SHARED),
        DECLINE(State.DECLINED, State.SHARED),
        CANCEL(State.CANCELED, State.ACCEPTED);

        @JsonIgnore
        private State nextState;
        @JsonIgnore
        private State[] currentStates;
        StateTransition(State nextState, @NotNull State... currentStates) {
            this.currentStates = currentStates;
            this.nextState = nextState;
        }
        @JsonIgnore
        public boolean willBeInFinalState() {
            return this.nextState.isFinalState();
        }
        public State getNextStateFrom(State from) throws IllegalStateException {
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
    private final Party client;


    @NotNull
    private final Map<String, Object> serviceData;
    @JsonIgnore
    private final Party serviceProvider;
    private final Integer price;

    @ConstructorForDeserialization
    public ServiceState(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party client, @NotNull State state, Map<String, Object> serviceData, Party serviceProvider, Integer price) {
        this.id = id;
        this.state = state;
        this.serviceName = serviceName;
        this.client = client;
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
        list.add(this.client);
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
    public Party getClient() {
        return client;
    }
    @NotNull
    public State getState() { return state; }
    public Party getServiceProvider() {
        return serviceProvider;
    }


    @NotNull
    public String getClientX500() {
        return client.getName().getX500Principal().getName();
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
    public static ServiceState create(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party client, Map<String, Object> serviceData) {
        return new ServiceState(id, serviceName, client, StateTransition.CREATE.getInitialState(), serviceData, null, null);
    }
    /* actions UPDATE */
    public ServiceState update(Map<String, Object> newServiceData) {
        return this.update(newServiceData, this.price);
    }
    public ServiceState update(Map<String, Object> newServiceData, Integer newPrice) {
        State newState = StateTransition.UPDATE.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, newServiceData, this.serviceProvider, newPrice);
    }
    /* actions SHARE */
    public ServiceState share(@NotNull Party newServiceProvider) {
        State newState = StateTransition.SHARE.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, this.serviceData, newServiceProvider, this.price);
    }
    /* actions WITHDRAW */
    public ServiceState withdraw() {
        State newState = StateTransition.WITHDRAW.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, this.serviceData, this.serviceProvider, this.price);
    }
    /* actions ACCEPT */
    public ServiceState accept() {
        State newState = StateTransition.ACCEPT.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, this.serviceData, this.serviceProvider, this.price);
    }
    /* actions DECLINE */
    public ServiceState decline() {
        State newState = StateTransition.DECLINE.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, this.serviceData, this.serviceProvider, this.price);
    }
    /* actions CANCEL */
    public ServiceState cancel() {
        State newState = StateTransition.CANCEL.getNextStateFrom(this.state);
        return new ServiceState(this.id, this.serviceName, this.client, newState, this.serviceData, this.serviceProvider, this.price);
    }

}
