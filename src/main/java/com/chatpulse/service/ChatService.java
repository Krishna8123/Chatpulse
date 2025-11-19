package com.chatpulse.service;

import com.chatpulse.model.*;
import com.chatpulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatRequestRepository chatRequestRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;

    public Chat createPrivateChatRequest(User sender, String receiverPhoneNumber) {
        Optional<User> receiverOpt = userRepository.findByPhoneNumber(receiverPhoneNumber);
        if (receiverOpt.isEmpty()) {
            throw new RuntimeException("User with phone number not found");
        }
        
        User receiver = receiverOpt.get();
        if (receiver.getId().equals(sender.getId())) {
            throw new RuntimeException("Cannot create chat with yourself");
        }
        
        // Check if request already exists
        Optional<ChatRequest> existingRequest = chatRequestRepository
            .findBySenderAndReceiverAndRequestTypeAndStatus(sender, receiver, "PRIVATE", "PENDING");
        if (existingRequest.isPresent()) {
            throw new RuntimeException("Request already sent");
        }
        
        // Check if chat already exists (check both directions)
        List<Chat> existingChats1 = chatRepository.findPrivateChatBetweenUsers(sender, receiver);
        List<Chat> existingChats2 = chatRepository.findPrivateChatBetweenUsers(receiver, sender);
        if (!existingChats1.isEmpty() || !existingChats2.isEmpty()) {
            throw new RuntimeException("Chat already exists");
        }
        
        // Create request
        ChatRequest request = new ChatRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setRequestType("PRIVATE");
        chatRequestRepository.save(request);
        
        return null; // Request created, chat will be created when accepted
    }

    @Transactional
    public Chat acceptPrivateChatRequest(Long requestId, User receiver) {
        Optional<ChatRequest> requestOpt = chatRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("Request not found");
        }
        
        ChatRequest request = requestOpt.get();
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request already processed");
        }
        
        // Create chat - name is set to receiver's username so sender can see it
        // Receiver will see it because createdBy is sender
        Chat chat = new Chat();
        chat.setType("PRIVATE");
        chat.setName(receiver.getUsername()); // Name shows the other person's username
        chat.setCreatedBy(request.getSender());
        chat = chatRepository.save(chat);
        
        // Update request status
        request.setStatus("ACCEPTED");
        chatRequestRepository.save(request);
        
        return chat;
    }

    public Chat createGroupChatRequest(User creator, String groupName, List<String> memberPhoneNumbers) {
        Chat chat = new Chat();
        chat.setType("GROUP");
        chat.setName(groupName);
        chat.setCreatedBy(creator);
        chat = chatRepository.save(chat);
        
        // Add creator as member
        GroupMember creatorMember = new GroupMember(chat, creator);
        groupMemberRepository.save(creatorMember);
        
        // Send requests to members
        for (String phoneNumber : memberPhoneNumbers) {
            Optional<User> memberOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (memberOpt.isPresent()) {
                User member = memberOpt.get();
                if (!member.getId().equals(creator.getId())) {
                    ChatRequest request = new ChatRequest();
                    request.setSender(creator);
                    request.setReceiver(member);
                    request.setRequestType("GROUP");
                    request.setGroupChat(chat);
                    request.setGroupName(groupName);
                    chatRequestRepository.save(request);
                }
            }
        }
        
        return chat;
    }

    @Transactional
    public void acceptGroupChatRequest(Long requestId, User receiver) {
        Optional<ChatRequest> requestOpt = chatRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("Request not found");
        }
        
        ChatRequest request = requestOpt.get();
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request already processed");
        }
        
        Chat groupChat = request.getGroupChat();
        if (groupChat == null) {
            throw new RuntimeException("Group chat not found");
        }
        
        // Add user to group
        if (!groupMemberRepository.existsByChatAndUser(groupChat, receiver)) {
            GroupMember member = new GroupMember(groupChat, receiver);
            groupMemberRepository.save(member);
        }
        
        // Update request status
        request.setStatus("ACCEPTED");
        chatRequestRepository.save(request);
    }

    public void rejectChatRequest(Long requestId, User receiver) {
        Optional<ChatRequest> requestOpt = chatRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("Request not found");
        }
        
        ChatRequest request = requestOpt.get();
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        request.setStatus("REJECTED");
        chatRequestRepository.save(request);
    }

    public List<ChatRequest> getPendingRequests(User user) {
        return chatRequestRepository.findPendingRequestsByReceiver(user);
    }

    public List<Chat> getPrivateChats(User user) {
        return chatRepository.findPrivateChatsByUser(user);
    }

    public List<Chat> getGroupChats(User user) {
        return chatRepository.findGroupChatsByUser(user);
    }

    public Optional<Chat> getChatById(Long chatId) {
        return chatRepository.findById(chatId);
    }
}

