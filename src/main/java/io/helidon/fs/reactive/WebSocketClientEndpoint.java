package io.helidon.fs.reactive;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import io.helidon.common.reactive.SequentialSubscriber;

public class WebSocketClientEndpoint extends Endpoint implements Flow.Publisher<String>, Flow.Subscription {

    private static final Logger LOGGER = Logger.getLogger(WebSocketClientEndpoint.class.getName());

    private Session session;
    private Flow.Subscriber<? super String> subscriber;

    @Override
    public void onOpen(final Session session, final EndpointConfig endpointConfig) {
        this.session = session;
        session.addMessageHandler(new MessageHandler.Whole<ReactiveSignal>() {
            @Override
            public void onMessage(ReactiveSignal signal) {
                switch (signal.type) {
                    case ON_NEXT:
                        subscriber.onNext(signal.item);
                        break;
                    case ON_ERROR:
                        subscriber.onError(signal.error);
                        break;
                    case ON_COMPLETE:
                        subscriber.onComplete();
                        break;
                    default:
                        subscriber.onError(new IllegalStateException("Unexpected signal " + signal.type + " from upstream!"));
                }
            }
        });
    }

    @Override
    public void onError(final Session session, final Throwable thr) {
        Optional.ofNullable(subscriber).ifPresent(s -> s.onError(thr));
        LOGGER.log(Level.SEVERE, thr, () -> "Connection error");
        super.onError(session, thr);
    }

    @Override
    public void onClose(final Session session, final CloseReason closeReason) {
        subscriber.onComplete();
        super.onClose(session, closeReason);
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super String> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        // Notice usage of Helidon's SequentialSubscriber as a wrapper
        // to get us around difficulties with specification rules 1.3, 1.7
        this.subscriber = SequentialSubscriber.create(subscriber);
        subscriber.onSubscribe(this);
    }

    @Override
    public void request(final long n) {
        sendAsyncSignal(ReactiveSignal.request(n));
    }

    @Override
    public void cancel() {
        sendAsyncSignal(ReactiveSignal.cancel());
    }

    private void sendAsyncSignal(ReactiveSignal signal) {
        try {
            //reactive means no blocking
            session.getAsyncRemote().sendObject(signal);
        } catch (Exception e) {
            subscriber.onError(e);
        }
    }
}
