package io.helidon.fs.reactive;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ReactiveSignalEncoderDecoder
        implements Encoder.TextStream<ReactiveSignal>, Decoder.TextStream<ReactiveSignal> {

    private static final Logger LOGGER = Logger.getLogger(ReactiveSignalEncoderDecoder.class.getName());

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public ReactiveSignal decode(final Reader reader) {
        return jsonb.fromJson(reader, ReactiveSignal.class);
    }

    @Override
    public void encode(final ReactiveSignal object, final Writer writer) throws IOException {
        String jsonMessage = jsonb.toJson(object);
        LOGGER.info(jsonMessage);
        writer.write(jsonMessage);
    }

    @Override
    public void init(final EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
