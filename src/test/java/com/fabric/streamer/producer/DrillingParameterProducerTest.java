package com.fabric.streamer.producer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fabric.streamer.model.DrillingParameter;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;

@ExtendWith(MockitoExtension.class)
class DrillingParameterProducerTest {

    @Mock
    MutinyEmitter<DrillingParameter> emitter;

    private DrillingParameterProducer producer;

    @BeforeEach
    void setUp() {
        producer = new DrillingParameterProducer(emitter);
    }

    @Test
    void pushTelemetryEmitsToKafkaChannel() {
        when(emitter.send(any(DrillingParameter.class))).thenAnswer(invocation -> {
            DrillingParameter payload = invocation.getArgument(0);
            assertNotNull(payload, "Payload should not be null");
            return Uni.createFrom().voidItem();
        });

        producer.pushTelemetry();

        verify(emitter).send(any(DrillingParameter.class));
    }
}
