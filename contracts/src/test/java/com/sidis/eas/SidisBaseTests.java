package com.sidis.eas;

import ch.cordalo.corda.common.test.CordaNodeEnvironment;
import ch.cordalo.corda.common.test.CordaTestNetwork;
import ch.cordalo.corda.common.test.CordaloBaseTests;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.FlowLogic;
import net.corda.testing.node.TestCordapp;

import java.util.List;

public abstract class SidisBaseTests extends CordaloBaseTests {

    public SidisBaseTests() {
    }

    protected CordaTestNetwork network;
    protected CordaNodeEnvironment insurance1;
    protected CordaNodeEnvironment insurance2;
    protected CordaNodeEnvironment fzl1;
    protected CordaNodeEnvironment fzl2;

    public List<String> getCordappPackageNames() {
        return ImmutableList.of(
                "com.sidis.eas.contracts",
                "ch.cordalo.corda.common.contracts"
        );
    }


    public void setup(boolean withNodes, Class<? extends FlowLogic> ...responderClasses) {
        this.network = new CordaTestNetwork(
                withNodes,
            this.getCordappPackageNames(),
            responderClasses
        );
        this.insurance1 = network.startEnv("Swisslife", "O=Swiss Life Ltd.,L=Zurich,ST=ZH,C=CH");
        this.insurance2 = network.startEnv("AXA", "O=AXA Leben AG,L=Winterthur,ST=ZH,C=CH");
        this.fzl1 = network.startEnv("FZL", "O=FZL,L=Zug,ST=ZG,C=CH");
        this.fzl2 = network.startEnv("Swisscanto", "O=Swisscanto Pensions Ltd.,L=Zurich,ST=ZH,C=CH");
        this.network.startNodes();
    }

    public void tearDown() {
        if (network != null) network.stopNodes();
    };
}
