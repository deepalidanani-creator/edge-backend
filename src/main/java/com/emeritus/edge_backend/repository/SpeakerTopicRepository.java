package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.SpeakerTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeakerTopicRepository extends JpaRepository<SpeakerTopic, Long> {

    boolean existsBySpeakerIdAndTopicId(Long speakerId, Long topicId);
}
