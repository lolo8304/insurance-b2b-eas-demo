package com.sidis.eas.states;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class EntryMessageState extends ServiceState {

    private String insuredCompany;
    private String insuredPerson;

    @ConstructorForDeserialization
    public EntryMessageState(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party initiator, @NotNull State state, String insuredCompany, String insuredPerson, Map<String, Object> serviceData, Party serviceProvider, Integer price) {
        super(id, serviceName, initiator, state, serviceData, serviceProvider, price);
        this.insuredCompany = insuredCompany;
        this.insuredPerson = insuredPerson;
    }

    public EntryMessageState(@NotNull UniqueIdentifier id, @NotNull String serviceName, @NotNull Party initiator, @NotNull State state, Map<String, Object> serviceData, Party serviceProvider, Integer price) {
        super(id, serviceName, initiator, state, serviceData, serviceProvider, price);
        this.insuredCompany = null;
        this.insuredPerson = null;
    }
}
