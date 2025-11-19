package com.chatpulse.repository;

import com.chatpulse.model.GroupMember;
import com.chatpulse.model.Chat;
import com.chatpulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByChat(Chat chat);
    List<GroupMember> findByUser(User user);
    Optional<GroupMember> findByChatAndUser(Chat chat, User user);
    boolean existsByChatAndUser(Chat chat, User user);
}

