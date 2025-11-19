package com.chatpulse.repository;

import com.chatpulse.model.ChatRequest;
import com.chatpulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {
    List<ChatRequest> findByReceiverAndStatus(User receiver, String status);
    
    @Query("SELECT cr FROM ChatRequest cr WHERE cr.receiver = :receiver AND cr.status = 'PENDING'")
    List<ChatRequest> findPendingRequestsByReceiver(@Param("receiver") User receiver);
    
    Optional<ChatRequest> findBySenderAndReceiverAndRequestTypeAndStatus(
        User sender, User receiver, String requestType, String status);
}

