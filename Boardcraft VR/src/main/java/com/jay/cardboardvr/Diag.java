package com.jay.cardboardvr;

import java.util.concurrent.atomic.AtomicInteger;

/** Counters shown in the settings status line so we can see what the mod is really doing. */
public class Diag {
    public static final AtomicInteger lookBlocked = new AtomicInteger();
    public static volatile double dx = 0;
}
