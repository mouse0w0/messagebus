package com.github.mouse0w0.messagebus;

import java.lang.reflect.Method;
import java.util.List;

public interface MessageBusExceptionHandler {
    void handle(Topic<?> topic, Method method, Object[] args, List<Throwable> throwables);
}
