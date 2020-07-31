package io.helidon.fs.reactive;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;

import javax.websocket.server.ServerEndpointConfig;

import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.tyrus.TyrusSupport;

public final class Server {

    private Server() {
    }

    public static void main(final String[] args) throws IOException {
        // load logging configuration
        setupLogging();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        WebServer server = WebServer.builder(createRouting(config))
                .config(config.get("server"))
                .build();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        server.start()
                .thenAccept(ws -> {
                    System.out.println(
                            "WEB server is up! http://localhost:" + ws.port());
                    ws.whenShutdown().thenRun(()
                            -> System.out.println("WEB server is DOWN. Good bye!"));
                })
                .exceptionally(t -> {
                    System.err.println("Startup failed: " + t.getMessage());
                    t.printStackTrace(System.err);
                    return null;
                });

        // Server threads are not daemon. No need to block. Just react.
    }

    private static Routing createRouting(Config config) {
        StreamFactory streamFactory = new StreamFactory();

        TyrusSupport tyrusSupport = TyrusSupport.builder()
                .register(
                        ServerEndpointConfig.Builder.create(
                                WebSocketServerEndpoint.class, "/messages")
                                .encoders(List.of(ReactiveSignalEncoderDecoder.class))
                                .decoders(List.of(ReactiveSignalEncoderDecoder.class))
                                .configurator(new ServerEndpointConfig.Configurator() {
                                    @Override
                                    public <T> T getEndpointInstance(final Class<T> endpointClass)
                                            throws InstantiationException {
                                        T endpointInstance = super.getEndpointInstance(endpointClass);
                                        if (endpointInstance instanceof WebSocketServerEndpoint) {
                                            WebSocketServerEndpoint endpoint = (WebSocketServerEndpoint) endpointInstance;
                                            //Endpoint is instantiated for every connection, lets subscribe it to our upstream
                                            streamFactory.createStream().subscribe(endpoint);
                                        }
                                        return endpointInstance;
                                    }
                                })
                                .build())
                .build();

        return Routing.builder()
                // register static content support (on "/")
                .register(StaticContentSupport.builder("/WEB").welcomeFileName("index.html"))
                // register WebSocket endpoint to push messages coming from Kafka to client
                .register("/ws", tyrusSupport)
                .build();
    }

    /**
     * Configure logging from logging.properties file.
     */
    private static void setupLogging() throws IOException {
        try (InputStream is = Server.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        }
    }
}
