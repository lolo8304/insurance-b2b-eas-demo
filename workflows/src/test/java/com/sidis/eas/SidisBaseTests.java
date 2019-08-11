package com.sidis.eas;

import com.google.common.collect.ImmutableList;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.*;
import org.junit.After;

import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

abstract public class SidisBaseTests {
    protected final Instant start = Instant.now();

    public static final CordaX500Name insurer1X500 = CordaX500Name.parse("O=Swiss Life Ltd.,L=Zurich,ST=ZH,C=CH");
    public static final CordaX500Name insurer2X500 = CordaX500Name.parse("O=AXA Leben AG,L=Winterthur,ST=ZH,C=CH");
    public static final CordaX500Name fzl1X500 = CordaX500Name.parse("O=FZL,L=Zug,ST=ZG,C=CH");
    public static final CordaX500Name fzl2X500 = CordaX500Name.parse("O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH");
    protected final TestIdentity insurer1ID = new TestIdentity(insurer1X500);
    protected final TestIdentity insurer2ID = new TestIdentity(insurer2X500);
    protected final TestIdentity fzl1ID = new TestIdentity(fzl1X500);
    protected MockNetwork network;
    protected StartedMockNode insurer1Node;
    protected Party insurer1Party;
    protected StartedMockNode insurer2Node;
    protected Party insurer2Party;
    protected StartedMockNode fzl1Node;
    protected Party fzl1Party;

    protected MockServices ledgerServices = null;
    protected MockServices ledgerServicesBroker = null;
    protected MockServices ledgerServicesInsurer = null;

    // must be called to initialize using setup(true | false) and annotate with @Before
    public abstract void setup();

    public void setup(boolean withNodes) {
        this.setup(withNodes, null);

    }
    public void setup(boolean withNodes, Class<? extends FlowLogic> responderClass) {

        if (withNodes) {
            network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
                    TestCordapp.findCordapp("com.sidis.eas.contracts")
            )));
            insurer1Node = network.createPartyNode(insurer1ID.getName());
            if (responderClass != null) insurer1Node.registerInitiatedFlow(responderClass);
            insurer2Node = network.createPartyNode(insurer2ID.getName());
            if (responderClass != null) insurer2Node.registerInitiatedFlow(responderClass);
            fzl1Node = network.createPartyNode(fzl1ID.getName());
            if (responderClass != null) fzl1Node.registerInitiatedFlow(responderClass);

            insurer1Party = insurer1Node.getInfo().getLegalIdentities().get(0);
            insurer2Party = insurer2Node.getInfo().getLegalIdentities().get(0);
            fzl1Party = fzl1Node.getInfo().getLegalIdentities().get(0);

            network.runNetwork();
        } else {
            insurer1Party = insurer1ID.getParty();
            insurer2Party = insurer2ID.getParty();
            fzl1Party = fzl1ID.getParty();
        }


        ledgerServices = new MockServices(
                ImmutableList.of("com.sidis.eas"),
                insurer1Party.getName()
        );
        ledgerServicesBroker = new MockServices(
                ImmutableList.of("com.sidis.eas"),
                insurer2Party.getName()
        );
        ledgerServicesInsurer = new MockServices(
                ImmutableList.of("com.sidis.eas"),
                fzl1Party.getName()
        );



    }

    @After
    public void tearDown() {
        if (network != null) network.stopNodes();
    }


    protected List<PublicKey> getPublicKeys(Party... parties) {
        ImmutableList<Party> list = ImmutableList.copyOf(parties);
        return list.stream().map(party -> party.getOwningKey()).collect(Collectors.toList());
    }

    protected Party getParty(StartedMockNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }

}
