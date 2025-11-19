package com.chatpulse.repository;

import com.chatpulse.model.Message;
import com.chatpulse.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatOrderByTimestampAsc(Chat chat);
    
    @Query("SELECT m FROM Message m WHERE m.chat = :chat ORDER BY m.timestamp ASC")
    List<Message> findAllByChatOrderByTimestamp(@Param("chat") Chat chat);
}

