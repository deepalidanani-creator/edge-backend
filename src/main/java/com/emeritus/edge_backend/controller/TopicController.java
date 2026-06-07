package com.emeritus.edge_backend.controller;

import com.emeritus.edge_backend.entity.Topic;
import com.emeritus.edge_backend.service.TopicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/v1/topics")
    public List<Topic> getTopics() {
        return topicService.getAllTopics();
    }
}
