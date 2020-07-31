
/*
 * Copyright (c)  2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.helidon.fs.reactive;

import java.io.IOException;
import java.util.concurrent.Flow;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class WebSocketServerEndpoint extends Endpoint implements Flow.Subscriber<String> {

    private static final Logger LOGGER = Logger.getLogger(WebSocketServerEndpoint.class.getName());

    private Session session;
    private Flow.Subscription subscription;

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("Session " + session.getId());

        session.addMessageHandler(new MessageHandler.Whole<ReactiveSignal>() {
            @Override
            public void onMessage(ReactiveSignal signal) {
                switch (signal.type) {
                    case REQUEST:
                        subscription.request(signal.requested);
                        break;
                    case CANCEL:
                        subscription.cancel();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected signal " + signal.type + " from upstream!");
                }
            }
        });
    }

    @Override
    public void onError(final Session session, final Throwable thr) {
        LOGGER.log(Level.SEVERE, thr, () -> "WebSocket error.");
        super.onError(session, thr);
    }

    @Override
    public void onClose(final Session session, final CloseReason closeReason) {
        super.onClose(session, closeReason);
        subscription.cancel();
    }

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(final String item) {
        sendSignal(ReactiveSignal.next(item));
    }

    @Override
    public void onError(final Throwable throwable) {
        sendSignal(ReactiveSignal.error(throwable));
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Error when closing web socket.");
        }
    }

    @Override
    public void onComplete() {
        sendSignal(ReactiveSignal.complete());
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Completed"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Error when closing web socket.");
        }
    }

    private void sendSignal(ReactiveSignal signal) {
        session.getAsyncRemote().sendObject(signal);
    }
}
