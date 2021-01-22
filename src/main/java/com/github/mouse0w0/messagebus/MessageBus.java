package com.github.mouse0w0.messagebus;

public interface MessageBus {
    MessageBus getParent();

    <T> T getPublisher(Topic<T> topic);

    <T> void subscribe(Topic<T> topic, T subscriber);

    <T> void unsubscribe(Topic<T> topic, T subscriber);
}
