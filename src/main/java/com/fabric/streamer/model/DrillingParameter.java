package com.fabric.streamer.model;

import java.time.Instant;

/**
 * Immutable drilling telemetry payload.
 */
public record DrillingParameter(double rpm, double weightOnBit, double torque, Instant timestamp) {
}
