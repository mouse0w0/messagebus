package com.github.mouse0w0.messagebus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageBusImpl implements MessageBus {
    protected final MessageBusImpl parent;

    protected final Map<Topic<?>, Object> publishers = new ConcurrentHashMap<>();
    protected final Map<Topic<?>, List<Object>> subscribers = new ConcurrentHashMap<>();

    protected final List<MessageBusImpl> children = new ArrayList<>(0);

    protected MessageBusExceptionHandler exceptionHandler;

    public MessageBusImpl() {
        this(null);
    }

    public MessageBusImpl(MessageBusImpl parent) {
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    synchronized void addChild(MessageBusImpl child) {
        children.add(child);
    }

    @Override
    public MessageBusImpl getParent() {
        return parent;
    }

    public MessageBusExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(MessageBusExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    protected void handleException(Topic<?> topic, Method method, Object[] args, List<Throwable> throwables) {
        if (exceptionHandler == null || throwables == null) return;
        exceptionHandler.handle(topic, method, args, throwables);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPublisher(Topic<T> topic) {
        return (T) publishers.computeIfAbsent(topic, this::createPublisher);
    }

    protected Object createPublisher(Topic<?> topic) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Topic.BroadcastDirection broadcastDirection = topic.getBroadcastDirection();
        if (broadcastDirection == Topic.BroadcastDirection.TO_CHILDREN) {
            return Proxy.newProxyInstance(classLoader, new Class[]{topic.getSubscriberClass()}, new ToChildrenMessagePublisher(this, topic));
        } else if (broadcastDirection == Topic.BroadcastDirection.TO_PARENT) {
            return Proxy.newProxyInstance(classLoader, new Class[]{topic.getSubscriberClass()}, new ToParentMessagePublisher(this, topic));
        } else if (broadcastDirection == Topic.BroadcastDirection.TO_DIRECT_CHILDREN) {
            return Proxy.newProxyInstance(classLoader, new Class[]{topic.getSubscriberClass()}, new ToDirectChildrenMessagePublisher(this, topic));
        } else {
            return Proxy.newProxyInstance(classLoader, new Class[]{topic.getSubscriberClass()}, new MessagePublisher(this, topic));
        }
    }

    @Override
    public <T> void subscribe(Topic<T> topic, T subscriber) {
        subscribers.computeIfAbsent(topic, t -> new ArrayList<>()).add(subscriber);
    }

    @Override
    public <T> void unsubscribe(Topic<T> topic, T subscriber) {
        List<Object> subscriberList = subscribers.get(topic);
        if (subscriberList != null) {
            subscriberList.remove(subscriber);
        }
    }

    protected static class MessagePublisher implements InvocationHandler {
        protected final MessageBusImpl messageBus;
        protected final Topic<?> topic;

        public MessagePublisher(MessageBusImpl messageBus, Topic<?> topic) {
            this.messageBus = messageBus;
            this.topic = topic;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("java.lang.Object".equals(method.getDeclaringClass().getName())) {
                return handleObjectMethod(proxy, method, args);
            }

            publish(proxy, method, args);
            return null;
        }

        public void publish(Object proxy, Method method, Object[] args) {
            List<Object> subscriberList = messageBus.subscribers.get(topic);
            if (subscriberList == null) return;
            List<Throwable> throwables = null;
            for (Object subscriber : subscriberList) {
                try {
                    method.invoke(subscriber, args);
                } catch (Throwable t) {
                    throwables = handleThrowable(t, throwables);
                }
            }

            messageBus.handleException(topic, method, args, throwables);
        }

        protected List<Throwable> handleThrowable(Throwable t, List<Throwable> throwables) {
            if (throwables == null) {
                throwables = new ArrayList<>();
            }
            throwables.add(t);
            return throwables;
        }

        protected Object handleObjectMethod(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("toString".equals(name)) {
                return "MessagePublisher{messageBus='" + messageBus + "', topic='" + topic + "'}";
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("equals".equals(name)) {
                return proxy == args[0];
            } else {
                throw new UnsupportedOperationException("Proxy");
            }
        }
    }

    protected static class ToChildrenMessagePublisher extends MessagePublisher {

        public ToChildrenMessagePublisher(MessageBusImpl messageBus, Topic<?> topic) {
            super(messageBus, topic);
        }

        @Override
        public void publish(Object proxy, Method method, Object[] args) {
            Queue<MessageBusImpl> queue = new ArrayDeque<>();
            queue.add(messageBus);

            MessageBusImpl bus;
            List<Throwable> throwables = null;
            while ((bus = queue.poll()) != null) {
                queue.addAll(bus.children);

                List<Object> subscriberList = bus.subscribers.get(topic);
                if (subscriberList == null) continue;
                for (Object subscriber : subscriberList) {
                    try {
                        method.invoke(subscriber, args);
                    } catch (Throwable t) {
                        throwables = handleThrowable(t, throwables);
                    }
                }
            }

            messageBus.handleException(topic, method, args, throwables);
        }
    }

    protected static class ToDirectChildrenMessagePublisher extends MessagePublisher {

        public ToDirectChildrenMessagePublisher(MessageBusImpl messageBus, Topic<?> topic) {
            super(messageBus, topic);
        }

        @Override
        public void publish(Object proxy, Method method, Object[] args) {
            List<Throwable> throwables = null;
            {
                List<Object> subscriberList = messageBus.subscribers.get(topic);
                if (subscriberList != null) {
                    for (Object subscriber : subscriberList) {
                        try {
                            method.invoke(subscriber, args);
                        } catch (Throwable t) {
                            throwables = handleThrowable(t, throwables);
                        }
                    }
                }
            }

            for (MessageBusImpl bus : messageBus.children) {
                List<Object> subscriberList = bus.subscribers.get(topic);
                if (subscriberList == null) continue;
                for (Object subscriber : subscriberList) {
                    try {
                        method.invoke(subscriber, args);
                    } catch (Throwable t) {
                        throwables = handleThrowable(t, throwables);
                    }
                }
            }

            messageBus.handleException(topic, method, args, throwables);
        }
    }

    protected static class ToParentMessagePublisher extends MessagePublisher {

        public ToParentMessagePublisher(MessageBusImpl messageBus, Topic<?> topic) {
            super(messageBus, topic);
        }

        @Override
        public void publish(Object proxy, Method method, Object[] args) {
            MessageBusImpl bus = messageBus;
            List<Throwable> throwables = null;
            do {
                List<Object> subscriberList = bus.subscribers.get(topic);
                if (subscriberList == null) continue;
                for (Object subscriber : subscriberList) {
                    try {
                        method.invoke(subscriber, args);
                    } catch (Throwable t) {
                        throwables = handleThrowable(t, throwables);
                    }
                }
                bus = bus.getParent();
            } while (bus != null);

            messageBus.handleException(topic, method, args, throwables);
        }
    }
}
