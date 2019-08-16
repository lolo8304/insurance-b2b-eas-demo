package com.sidis.eas.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseFlow extends FlowLogic<SignedTransaction> {
    public BaseFlow() {
        super();
    }


    protected TransactionBuilder getTransactionBuilderSignedBySigners(ImmutableList<PublicKey> requiredSigner, CommandData command) throws FlowException {
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(getFirstNotary());
        transactionBuilder.addCommand(command, requiredSigner);
        return transactionBuilder;
    }

    protected TransactionBuilder getTransactionBuilderSignedByParticipants(ContractState state, CommandData command) throws FlowException {
        List<PublicKey> publicKeys = state.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
        ImmutableList<PublicKey> requiredSigner = new ImmutableList.Builder<PublicKey>()
                .addAll(publicKeys)
                .build();
        return getTransactionBuilderSignedBySigners(requiredSigner, command);
    }

    protected TransactionBuilder getMyTransactionBuilderSignedByMe(CommandData command) throws FlowException {
        return getTransactionBuilderSignedBySigners(
                ImmutableList.of(getOurIdentity().getOwningKey()),
                command);
    }

    @Suspendable
    protected SignedTransaction signAndFinalize(TransactionBuilder transactionBuilder) throws FlowException {
        return this.signSyncCollectAndFinalize(false, Collections.EMPTY_SET, transactionBuilder);
    }

    @Suspendable
    protected SignedTransaction signSyncCollectAndFinalize(Party counterparty, TransactionBuilder transactionBuilder) throws FlowException {
        Set<Party> set = Collections.EMPTY_SET;
        if (counterparty != null) {
            set = Collections.singleton(counterparty);
        }
        return signSyncCollectAndFinalize(set, transactionBuilder);
    }
    @Suspendable
    protected SignedTransaction signSyncCollectAndFinalize(Set<Party> counterparties, TransactionBuilder transactionBuilder) throws FlowException {
        return signSyncCollectAndFinalize(true, counterparties, transactionBuilder);
    }


    @Suspendable
    protected SignedTransaction signCollectAndFinalize(Party counterparty, TransactionBuilder transactionBuilder) throws FlowException {
        Set<Party> set = Collections.EMPTY_SET;
        if (counterparty != null) {
            set = Collections.singleton(counterparty);
        }
        return signCollectAndFinalize(set, transactionBuilder);
    }

    @Suspendable
    protected SignedTransaction signCollectAndFinalize(Set<Party> counterparties, TransactionBuilder transactionBuilder) throws FlowException {
        return signSyncCollectAndFinalize(false, counterparties, transactionBuilder);
    }

        @Suspendable
    private SignedTransaction signSyncCollectAndFinalize(boolean syncIdentities, Set<Party> counterparties, TransactionBuilder transactionBuilder) throws FlowException {
        ProgressTracker tracker = this.getProgressTracker();
        tracker.setCurrentStep(VERIFYING);
        transactionBuilder.verify(getServiceHub());

        tracker.setCurrentStep(SIGNING);
        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        Integer otherPartiesFlowVersion = 2;
        Set<FlowSession> otherPartySessions = new HashSet<>();

        //sync and collect counterparties only if counterparties exist
        if (counterparties != null && !counterparties.isEmpty()) {
            // prepare counterparties flow sessions to sync and / or collec
            for(Party counterparty : counterparties) {
                FlowSession flowSession = initiateFlow(counterparty);
                otherPartiesFlowVersion = flowSession.getCounterpartyFlowInfo().getFlowVersion();
                otherPartySessions.add(flowSession);
            }

            // Send any keys and certificates so the signers can verify each other's identity
            if (syncIdentities) {
                tracker.setCurrentStep(SYNCING);
                subFlow(new IdentitySyncFlow.Send(otherPartySessions, signedTx.getTx(), SYNCING.childProgressTracker()));
            }

            // Send the state to all counterparties, and receive it back with their signature.
            tracker.setCurrentStep(COLLECTING);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(
                            signedTx,
                            otherPartySessions,
                            ImmutableSet.of(getOurIdentity().getOwningKey()),
                            COLLECTING.childProgressTracker()));
            signedTx = fullySignedTx;
        }
        // We get the transaction notarised and recorded automatically by the platform.
        // send a copy to current issuer
        tracker.setCurrentStep(FINALISING);
        if (otherPartiesFlowVersion == 1) {
            return subFlow(new FinalityFlow(signedTx, Collections.emptyList(), FINALISING.childProgressTracker()));
        } else {
            return subFlow(new FinalityFlow(signedTx, otherPartySessions, FINALISING.childProgressTracker()));
        }
    }

    protected Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available notary.");
        }
        return notaries.get(0);
    }

    protected <T extends ContractState> StateAndRef<T> getLastStateByLinearId(Class<T> stateClass, UniqueIdentifier linearId) throws FlowException {
        StateAndRef stateRef = new FlowHelper<T>(getServiceHub()).getLastStateByLinearId(stateClass, linearId);
        if (stateRef == null) {
            throw new FlowException(String.format("State of class '%s' with id %s not found.", stateClass.getName(), linearId));
        }
        return stateRef;
    }

    protected <T extends ContractState>T getStateByRef(StateAndRef<T> ref){
        return ref.getState().getData();
    }

    protected final ProgressTracker.Step PREPARATION = new ProgressTracker.Step("Obtaining data from vault.");
    protected final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building transaction.");
    protected final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verifying transaction.");
    protected final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    protected final ProgressTracker.Step SYNCING = new ProgressTracker.Step("Syncing identities.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return IdentitySyncFlow.Send.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    protected final ProgressTracker progressTracker_sync = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            SYNCING,        // + Identity Sync Flow: Unit / Void
            COLLECTING,     // + Collect Signatures Flow: SignedTransaction
            FINALISING      // + Finality Flow: SignedTransaction
    );
    protected final ProgressTracker progressTracker_nosync = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            COLLECTING,     // + Collect Signatures Flow: SignedTransaction
            FINALISING      // + Finality Flow: SignedTransaction
    );
    protected final ProgressTracker progressTracker_nosync_nocollect = new ProgressTracker(
            PREPARATION,    // none
            BUILDING,       // none
            VERIFYING,      // none
            SIGNING,        // none
            FINALISING      // + Finality Flow: SignedTransaction
    );

    @Override
    public abstract ProgressTracker getProgressTracker();

    public static class SignTxFlowNoChecking extends SignTransactionFlow {
        public SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Suspendable
        @Override
        public void checkTransaction(SignedTransaction tx) {
            // no checking
        }
    }


}
