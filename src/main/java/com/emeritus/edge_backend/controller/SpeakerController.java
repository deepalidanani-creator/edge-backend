package com.emeritus.edge_backend.controller;

import com.emeritus.edge_backend.entity.Speaker;
import com.emeritus.edge_backend.service.SpeakerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class SpeakerController {

    private final SpeakerService speakerService;

    public SpeakerController(SpeakerService speakerService) {
        this.speakerService = speakerService;
    }

    @GetMapping("/v1/speakers")
    public List<Speaker> getSpeakers(@RequestParam(value = "topic", required = false) Long topicId) {
        return speakerService.getSpeakers(topicId);
    }
}
