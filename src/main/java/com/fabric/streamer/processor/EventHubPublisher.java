package com.fabric.streamer.processor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClientBuilder;
import com.fabric.streamer.model.DrillingParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Mono;

/**
 * Handles interaction with Azure Event Hubs and exposes a Mutiny friendly API.
 */
@ApplicationScoped
public class EventHubPublisher {

    private static final Logger LOG = Logger.getLogger(EventHubPublisher.class);

    private final ObjectMapper mapper;
    private final boolean enabled;
    private final Optional<String> connectionString;
    private final String hubName;

    private EventHubProducerAsyncClient client;

    @Inject
    public EventHubPublisher(ObjectMapper mapper,
            @ConfigProperty(name = "eventhubs.enabled", defaultValue = "false") boolean enabled,
            @ConfigProperty(name = "eventhubs.connection-string") Optional<String> connectionString,
            @ConfigProperty(name = "eventhubs.hub-name", defaultValue = "drilling-telemetry") String hubName) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.connectionString = connectionString;
        this.hubName = hubName;
    }

    @PostConstruct
    void init() {
        if (!enabled) {
            LOG.warn("Event Hubs forwarding disabled. Set eventhubs.enabled=true and provide credentials to enable the bridge.");
            return;
        }

        String resolvedConnection = connectionString.orElseThrow(() ->
            new IllegalStateException("eventhubs.connection-string must be provided when Event Hubs is enabled"));
        client = new EventHubProducerClientBuilder()
            .connectionString(resolvedConnection, hubName)
                .buildAsyncClient();
        LOG.infof("Connected Event Hub producer for hub %s", hubName);
    }

    public Uni<Void> send(List<DrillingParameter> payloads) {
        if (!enabled) {
            payloads.forEach(payload -> LOG.infof("[DRY RUN] %s", payload));
            return Uni.createFrom().voidItem();
        }
        if (client == null) {
            return Uni.createFrom().failure(new IllegalStateException("Event Hub client has not been initialised"));
        }

        List<EventData> events = payloads.stream()
                .map(this::toEvent)
                .toList();

        Mono<Void> sendMono = client.send(events);
        return Uni.createFrom().publisher(sendMono)
                .ifNoItem().after(Duration.ofSeconds(10)).fail()
                .replaceWithVoid();
    }

    private EventData toEvent(DrillingParameter payload) {
        try {
            byte[] json = mapper.writeValueAsBytes(payload);
            return new EventData(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise drilling payload", e);
        }
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            client.close();
        }
    }
}
