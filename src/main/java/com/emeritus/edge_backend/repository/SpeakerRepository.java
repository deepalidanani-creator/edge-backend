package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpeakerRepository extends JpaRepository<Speaker, Long> {

    @Query("SELECT s FROM Speaker s WHERE s.id IN "
         + "(SELECT st.speakerId FROM SpeakerTopic st WHERE st.topicId = :topicId)")
    List<Speaker> findByTopicId(@Param("topicId") Long topicId);
}
