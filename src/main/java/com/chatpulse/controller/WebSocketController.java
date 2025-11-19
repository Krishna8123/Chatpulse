package com.chatpulse.controller;

import com.chatpulse.model.Message;
import com.chatpulse.model.User;
import com.chatpulse.repository.UserRepository;
import com.chatpulse.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{chatId}")
    @SendTo("/topic/chat/{chatId}")
    public Map<String, Object> sendMessage(@DestinationVariable Long chatId, 
                                          Map<String, Object> messageData) {
        try {
            Long senderId = Long.parseLong(messageData.get("senderId").toString());
            String content = (String) messageData.get("content");
            String messageType = messageData.get("messageType") != null ? 
                               messageData.get("messageType").toString() : "TEXT";
            String filePath = messageData.get("filePath") != null ? 
                            messageData.get("filePath").toString() : null;

            // Fetch user from database
            Optional<User> userOpt = userRepository.findById(senderId);
            if (userOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "User not found");
                return error;
            }
            
            User sender = userOpt.get();

            // Save message to database
            Message message = messageService.sendMessage(chatId, sender, content, messageType, filePath);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", message.getId());
            response.put("chatId", chatId);
            Map<String, Object> senderMap = new HashMap<>();
            senderMap.put("id", sender.getId());
            senderMap.put("username", sender.getUsername());
            response.put("sender", senderMap);
            response.put("content", message.getContent());
            response.put("messageType", message.getMessageType());
            response.put("filePath", message.getFilePath());
            response.put("timestamp", message.getTimestamp().toString());

            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}

