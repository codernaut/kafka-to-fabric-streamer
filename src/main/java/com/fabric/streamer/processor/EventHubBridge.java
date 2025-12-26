package com.fabric.streamer.processor;

import java.util.Collections;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.fabric.streamer.model.DrillingParameter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Consumes drilling parameters from Kafka and forwards them into Azure Event Hubs.
 */
@ApplicationScoped
public class EventHubBridge {

    private static final Logger LOG = Logger.getLogger(EventHubBridge.class);

    private final EventHubPublisher publisher;

    public EventHubBridge(EventHubPublisher publisher) {
        this.publisher = publisher;
    }

    @Incoming("drilling-parameters-in")
    public Uni<Void> forward(Message<DrillingParameter> message) {
        DrillingParameter payload = message.getPayload();

        Uni<Void> send = publisher.send(Collections.singletonList(payload))
            .onItem().invoke(() -> LOG.debugf("Forwarded drilling payload to Event Hubs: %s", payload))
            .onFailure().invoke(throwable -> LOG.errorf(throwable, "Failed to push payload to Event Hubs"));

        return send
            .call(() -> Uni.createFrom().completionStage(message::ack))
            .onFailure().call(throwable -> Uni.createFrom().completionStage(() -> message.nack(throwable)))
            .replaceWithVoid();
    }
}
