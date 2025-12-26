package com.fabric.streamer.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fabric.streamer.model.DrillingParameter;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class EventHubBridgeTest {

    @Mock
    EventHubPublisher publisher;

    private EventHubBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new EventHubBridge(publisher);
    }

    @Test
    void forwardsMessagesAndAcksOnSuccess() {
        when(publisher.send(anyList())).thenReturn(Uni.createFrom().voidItem());

        AtomicBoolean acked = new AtomicBoolean();
        AtomicBoolean nacked = new AtomicBoolean();

        Message<DrillingParameter> message = messageWithHooks(acked, nacked);

        bridge.forward(message).await().indefinitely();

        assertTrue(acked.get(), "Message should be acked on success");
        assertFalse(nacked.get(), "Message should not be nacked on success");
        verify(publisher).send(anyList());
    }

    @Test
    void nacksMessagesWhenEventHubFails() {
        when(publisher.send(anyList())).thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));

        AtomicBoolean acked = new AtomicBoolean();
        AtomicBoolean nacked = new AtomicBoolean();

        Message<DrillingParameter> message = messageWithHooks(acked, nacked);

        assertThrows(RuntimeException.class, () -> bridge.forward(message).await().indefinitely());
        assertFalse(acked.get(), "Message should not be acked on failure");
        assertTrue(nacked.get(), "Message should be nacked on failure");
    }

    private Message<DrillingParameter> messageWithHooks(AtomicBoolean acked, AtomicBoolean nacked) {
        return Message.of(
                new DrillingParameter(100.0, 2_000.0, 3_000.0, Instant.now()),
                () -> {
                    acked.set(true);
                    return CompletableFuture.completedFuture(null);
                },
                failure -> {
                    nacked.set(true);
                    return CompletableFuture.completedFuture(null);
                });
    }
}
