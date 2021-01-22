package com.github.mouse0w0.messagebus;

public final class Topic<T> {
    private final String name;
    private final Class<T> subscriberClass;
    private final BroadcastDirection broadcastDirection;

    public enum BroadcastDirection {
        NONE,
        TO_CHILDREN,
        TO_PARENT,
        TO_DIRECT_CHILDREN
    }

    public Topic(Class<T> subscriberClass) {
        this(subscriberClass.getSimpleName(), subscriberClass, BroadcastDirection.TO_CHILDREN);
    }

    public Topic(String name, Class<T> subscriberClass) {
        this(name, subscriberClass, BroadcastDirection.TO_CHILDREN);
    }

    public Topic(String name, Class<T> subscriberClass, BroadcastDirection broadcastDirection) {
        this.name = name;
        this.subscriberClass = subscriberClass;
        this.broadcastDirection = broadcastDirection;
    }

    public String getName() {
        return name;
    }

    public Class<T> getSubscriberClass() {
        return subscriberClass;
    }

    public BroadcastDirection getBroadcastDirection() {
        return broadcastDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Topic<?> topic = (Topic<?>) o;

        if (!name.equals(topic.name)) return false;
        if (!subscriberClass.equals(topic.subscriberClass)) return false;
        return broadcastDirection == topic.broadcastDirection;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + subscriberClass.hashCode();
        result = 31 * result + broadcastDirection.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "name='" + name + '\'' +
                ", subscriberClass=" + subscriberClass +
                ", broadcastDirection=" + broadcastDirection +
                '}';
    }
}
