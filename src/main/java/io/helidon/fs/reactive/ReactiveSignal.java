package io.helidon.fs.reactive;

public class ReactiveSignal {

    public static ReactiveSignal request(long n) {
        ReactiveSignal signal = new ReactiveSignal();
        signal.type = Type.REQUEST;
        signal.requested = n;
        return signal;
    }

    public static ReactiveSignal cancel() {
        ReactiveSignal signal = new ReactiveSignal();
        signal.type = Type.CANCEL;
        return signal;
    }

    public static ReactiveSignal next(String item) {
        ReactiveSignal signal = new ReactiveSignal();
        signal.type = Type.ON_NEXT;
        signal.item = item;
        return signal;
    }

    public static ReactiveSignal error(Throwable t) {
        ReactiveSignal signal = new ReactiveSignal();
        signal.type = Type.ON_ERROR;
        signal.error = t;
        return signal;
    }

    public static ReactiveSignal complete() {
        ReactiveSignal signal = new ReactiveSignal();
        signal.type = Type.ON_COMPLETE;
        return signal;
    }

    public Type type;
    public Long requested;
    public String item;
    public Throwable error;

    public enum Type {
        REQUEST,
        CANCEL,
        ON_NEXT,
        ON_ERROR,
        ON_COMPLETE
    }
}
