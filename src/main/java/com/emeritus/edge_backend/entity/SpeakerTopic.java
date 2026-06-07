package com.emeritus.edge_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "speaker_topic", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"speakerId", "topicId"})
})
public class SpeakerTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long speakerId;
    private Long topicId;

    public SpeakerTopic() {}

    public SpeakerTopic(Long speakerId, Long topicId) {
        this.speakerId = speakerId;
        this.topicId = topicId;
    }

    public Long getId() { return id; }
    public Long getSpeakerId() { return speakerId; }
    public void setSpeakerId(Long speakerId) { this.speakerId = speakerId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
}
