package io.helidon.fs.reactive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Session;

import io.helidon.common.reactive.Multi;

import org.glassfish.tyrus.client.ClientManager;

public class Client {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    public static void main(String[] args)
            throws URISyntaxException, DeploymentException, InterruptedException, ExecutionException {

        ClientManager client = ClientManager.createClient();
        WebSocketClientEndpoint endpoint = new WebSocketClientEndpoint();

        Future<Session> sessionFuture = client.asyncConnectToServer(endpoint,
                ClientEndpointConfig.Builder
                        .create()
                        .encoders(List.of(ReactiveSignalEncoderDecoder.class))
                        .decoders(List.of(ReactiveSignalEncoderDecoder.class))
                        .build(),
                new URI("ws://localhost:8080/ws/messages"));

        //Wait for the connection
        sessionFuture.get();

        //Subscribe to our publisher and wait for the stream to end
        Multi.create(endpoint)
                .onError(throwable -> LOGGER.log(Level.SEVERE, throwable, () -> "Error from upstream!"))
                .onComplete(() -> LOGGER.log(Level.INFO, "Complete signal received!"))
                .forEach(s -> System.out.println("Received item> " + s))
                .await();
    }
}
