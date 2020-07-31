package io.helidon.fs.reactive;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.helidon.common.reactive.Multi;

public class StreamFactory {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    public Multi<String> createStream() {
        return Multi.interval(500, TimeUnit.MILLISECONDS, scheduledExecutorService)
                .limit(10)
                .map(aLong -> "Message number: " + aLong);
    }
}
