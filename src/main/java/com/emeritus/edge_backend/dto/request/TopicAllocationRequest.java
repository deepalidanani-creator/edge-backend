package com.emeritus.edge_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TopicAllocationRequest(
    Long topicId,
    @JsonProperty("allocatedSlots")
    @JsonAlias("slots")
    int allocatedSlots
) {}
