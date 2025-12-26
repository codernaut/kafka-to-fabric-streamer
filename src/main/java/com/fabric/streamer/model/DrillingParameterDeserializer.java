package com.fabric.streamer.model;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/**
 * Kafka JSON deserializer binding.
 */
public class DrillingParameterDeserializer extends ObjectMapperDeserializer<DrillingParameter> {

    public DrillingParameterDeserializer() {
        super(DrillingParameter.class);
    }
}
