package com.chatpulse.repository;

import com.chatpulse.model.Chat;
import com.chatpulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByTypeAndCreatedBy(String type, User createdBy);
    
    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND (c.createdBy = :user OR c.name = (SELECT u.username FROM User u WHERE u = :user))")
    List<Chat> findPrivateChatsByUser(@Param("user") User user);
    
    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND ((c.createdBy = :user1 AND c.name = (SELECT u.username FROM User u WHERE u = :user2)) OR (c.createdBy = :user2 AND c.name = (SELECT u.username FROM User u WHERE u = :user1)))")
    List<Chat> findPrivateChatBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    @Query("SELECT c FROM Chat c JOIN c.members m WHERE c.type = 'GROUP' AND m.user = :user")
    List<Chat> findGroupChatsByUser(@Param("user") User user);
}

