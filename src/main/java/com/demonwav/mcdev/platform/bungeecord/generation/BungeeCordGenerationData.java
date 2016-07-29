package com.demonwav.mcdev.platform.bungeecord.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;

import org.jetbrains.annotations.NotNull;

public class BungeeCordGenerationData implements GenerationData {
    private final String eventPriority;

    public BungeeCordGenerationData(@NotNull String eventPriority) {
        this.eventPriority = eventPriority;
    }

    public String getEventPriority() {
        return eventPriority;
    }
}
