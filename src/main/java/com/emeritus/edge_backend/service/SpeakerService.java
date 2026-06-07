package com.emeritus.edge_backend.service;

import com.emeritus.edge_backend.entity.Speaker;
import com.emeritus.edge_backend.repository.SpeakerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SpeakerService {

    private final SpeakerRepository speakerRepository;

    public SpeakerService(SpeakerRepository speakerRepository) {
        this.speakerRepository = speakerRepository;
    }

    @Transactional(readOnly = true)
    public List<Speaker> getSpeakers(Long topicId) {
        if (topicId == null) {
            return speakerRepository.findAll();
        }
        return speakerRepository.findByTopicId(topicId);
    }
}
