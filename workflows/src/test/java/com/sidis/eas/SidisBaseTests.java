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
import java.util.ArrayList;
import java.util.List;

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
        this.setup(withNodes, (Class<? extends FlowLogic>[]) null);

    }
    public void setup(boolean withNodes, Class<? extends FlowLogic> ...responderClasses) {

        if (withNodes) {
            network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
                    TestCordapp.findCordapp("com.sidis.eas.contracts")
            )));
            insurer1Node = network.createPartyNode(insurer1ID.getName());
            this.registerResponders(insurer1Node, responderClasses);
            insurer2Node = network.createPartyNode(insurer2ID.getName());
            this.registerResponders(insurer2Node, responderClasses);
            fzl1Node = network.createPartyNode(fzl1ID.getName());
            this.registerResponders(fzl1Node, responderClasses);

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

    private void registerResponders(StartedMockNode partyNode, Class<? extends FlowLogic>[] responderClasses) {
        if (responderClasses != null) {
            for (Class<? extends FlowLogic> responderClass: responderClasses) {
                partyNode.registerInitiatedFlow(responderClass);
            }
        }
    }

    @After
    public void tearDown() {
        if (network != null) network.stopNodes();
    }

    protected List<PublicKey> getPublicKeys(Party... parties) {
        List<PublicKey> publicKeys = new ArrayList<>();
        for (Party party: parties) {
            publicKeys.add(party.getOwningKey());
        }
        return publicKeys;
    }

    protected Party getParty(StartedMockNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }

}
