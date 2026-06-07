package com.emeritus.edge_backend.dto.response;

import com.emeritus.edge_backend.entity.Session;

public record ScheduleSessionResult(Session session, boolean newlyCreated) {}
