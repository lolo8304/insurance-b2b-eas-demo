package com.sidis.eas.states;

import com.sidis.eas.states.ServiceState.State;
import com.sidis.eas.states.ServiceState.StateTransition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class StateMachineTests {

    @Before
    public void setup() {
        State.values();
        StateTransition.values();
    }

    @Test
    public void testInitial() {
        State state = State.valueOf("CREATED");
        Assert.assertEquals("created is a valid state", State.CREATED, state);
        Assert.assertEquals("created is initial", true, state.isInitialState());
        Assert.assertEquals("created is not final", false, state.isFinalState());
    }

    @Test
    public void testActions() {
        State state = State.valueOf("CREATED");
        Assert.assertEquals("no valid next actions", Arrays.asList("REGISTER", "INFORM", "UPDATE", "NO_SHARE", "DUPLICATE", "SHARE", "WITHDRAW"), state.getNextActions());
    }

}
