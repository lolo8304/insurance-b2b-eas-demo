package com.sidis.eas.states;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sidis.eas.contracts.PatientRecordContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@BelongsToContract(PatientRecordContract.class)
@CordaSerializable
public class PatientRecordState implements LinearState {

    @JsonIgnore
    private final Party patient;

    private final UniqueIdentifier id;

    private final Map<String, Object> dataObject;


    @ConstructorForDeserialization
    public PatientRecordState(Party patient, UniqueIdentifier id, Map<String, Object> dataObject) {
        this.patient = patient;
        this.id = id;
        this.dataObject = dataObject;
    }

    public PatientRecordState(Party patient, UniqueIdentifier id, String data, Map<String, Object> dataObject) {
        this.patient = patient;
        this.id = id;
        this.dataObject = dataObject;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    public Party getPatient() {
        return patient;
    }

    public Map<String, Object> getDataObject() { return this.dataObject; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.id;
    }

    @NotNull
    @JsonIgnore
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(this.getPatient());
    }

    @NotNull
    @JsonIgnore
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    public String getPatientX500() {
        return patient.getName().getX500Principal().getName();
    }

    @JsonIgnore
    public Map<String, Object> filteredDataObject(String[] ids) {
        return (ids != null) ? JsonHelper.filterByGroupId(this.dataObject, ids) : null;
    }

    public PatientRecordState updatePatientRecord(String data) {
        return new PatientRecordState(this.patient, this.id, data, JsonHelper.convertStringToJson(data));
    }

    public PatientRecordState updateValues(String dataUpdateString) {
        Map<String, Object> newMap = JsonHelper.updateValues(this.getDataObject(), dataUpdateString);
        return new PatientRecordState(this.patient, this.id, JsonHelper.convertJsonToString(newMap), newMap);
    }

    public String getWalletItem(String walletName) {
        return JsonHelper.getDataValue(this.getDataObject(), "wallet."+walletName);
    }
    public String getDataValue(String attributesSeperatedByPoint) {
        return JsonHelper.getDataValue(this.getDataObject(), attributesSeperatedByPoint);
    }

}
