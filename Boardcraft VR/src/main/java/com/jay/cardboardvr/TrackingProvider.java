package com.jay.cardboardvr;

/**
 * Tracking abstraction. Today only the phone gyro (head rotation) implements it.
 * Future 6DoF controller tracking should add a new provider (e.g. left/right controller
 * pose, buttons, trigger, grip, joystick) without touching InputHandler or the screens.
 */
public interface TrackingProvider {
    boolean isActive();
    float yawDeg();
    float pitchDeg();
    /** True only when a provider really supplies tracked controllers. Never fake this. */
    default boolean supportsControllers() { return false; }
}
