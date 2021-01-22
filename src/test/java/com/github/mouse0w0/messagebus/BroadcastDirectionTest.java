package com.github.mouse0w0.messagebus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BroadcastDirectionTest {

    private MessageBusImpl depth0;
    private MessageBusImpl depth1;
    private MessageBusImpl depth2;
    private MessageBusImpl depth3;
    private MessageBusImpl depth4;

    private boolean visitedDepth0;
    private boolean visitedDepth1;
    private boolean visitedDepth2;
    private boolean visitedDepth3;
    private boolean visitedDepth4;

    public void run(Topic.BroadcastDirection broadcastDirection) {
        Topic<EventListener> topic = new Topic<>("Event", EventListener.class, broadcastDirection);

        visitedDepth0 = false;
        visitedDepth1 = false;
        visitedDepth2 = false;
        visitedDepth3 = false;
        visitedDepth4 = false;

        depth0 = new MessageBusImpl();
        depth1 = new MessageBusImpl(depth0);
        depth2 = new MessageBusImpl(depth1);
        depth3 = new MessageBusImpl(depth2);
        depth4 = new MessageBusImpl(depth3);

        depth0.subscribe(topic, arg0 -> visitedDepth0 = true);
        depth1.subscribe(topic, arg0 -> visitedDepth1 = true);
        depth2.subscribe(topic, arg0 -> visitedDepth2 = true);
        depth3.subscribe(topic, arg0 -> visitedDepth3 = true);
        depth4.subscribe(topic, arg0 -> visitedDepth4 = true);

        depth2.getPublisher(topic).onEvent("Test");
    }

    @Test
    public void testBroadcastNone() {
        run(Topic.BroadcastDirection.NONE);

        Assertions.assertFalse(visitedDepth0);
        Assertions.assertFalse(visitedDepth1);
        Assertions.assertTrue(visitedDepth2);
        Assertions.assertFalse(visitedDepth3);
        Assertions.assertFalse(visitedDepth4);
    }

    @Test
    public void testBroadcastToChildren() {
        run(Topic.BroadcastDirection.TO_CHILDREN);

        Assertions.assertFalse(visitedDepth0);
        Assertions.assertFalse(visitedDepth1);
        Assertions.assertTrue(visitedDepth2);
        Assertions.assertTrue(visitedDepth3);
        Assertions.assertTrue(visitedDepth4);
    }

    @Test
    public void testBroadcastToDirectChildren() {
        run(Topic.BroadcastDirection.TO_DIRECT_CHILDREN);

        Assertions.assertFalse(visitedDepth0);
        Assertions.assertFalse(visitedDepth1);
        Assertions.assertTrue(visitedDepth2);
        Assertions.assertTrue(visitedDepth3);
        Assertions.assertFalse(visitedDepth4);
    }

    @Test
    public void testBroadcastToParent() {
        run(Topic.BroadcastDirection.TO_PARENT);

        Assertions.assertTrue(visitedDepth0);
        Assertions.assertTrue(visitedDepth1);
        Assertions.assertTrue(visitedDepth2);
        Assertions.assertFalse(visitedDepth3);
        Assertions.assertFalse(visitedDepth4);
    }
}