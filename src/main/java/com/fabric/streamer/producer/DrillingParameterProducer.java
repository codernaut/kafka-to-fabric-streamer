package com.fabric.streamer.producer;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.jboss.logging.Logger;

import com.fabric.streamer.model.DrillingParameter;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.annotations.OnOverflow.Strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;

/**
 * Periodically publishes synthetic drilling parameters into Kafka.
 */
@ApplicationScoped
public class DrillingParameterProducer {

    private static final Logger LOG = Logger.getLogger(DrillingParameterProducer.class);

    private final MutinyEmitter<DrillingParameter> emitter;

    @Inject
    public DrillingParameterProducer(
            @Channel("drilling-parameters")
            @OnOverflow(value = Strategy.BUFFER, bufferSize = 4096)
            MutinyEmitter<DrillingParameter> emitter) {
        this.emitter = emitter;
    }

    @Scheduled(every = "0.1s")
    void pushTelemetry() {
        DrillingParameter payload = new DrillingParameter(
                rpm(),
                weightOnBit(),
                torque(),
                Instant.now());

        Cancellable acknowledgement = emitter.send(payload)
                .subscribe().with(
                        ignored -> {},
                        failure -> LOG.warn("Failed to enqueue drilling parameter", failure));

        if (acknowledgement == null) {
            LOG.debugf("Buffered drilling payload %s", payload);
        }
    }

    private double rpm() {
        return ThreadLocalRandom.current().nextDouble(80.0, 220.0);
    }

    private double weightOnBit() {
        return ThreadLocalRandom.current().nextDouble(5_000.0, 35_000.0);
    }

    private double torque() {
        return ThreadLocalRandom.current().nextDouble(2_000.0, 12_000.0);
    }
}
