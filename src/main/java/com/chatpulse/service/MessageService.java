package com.chatpulse.service;

import com.chatpulse.model.Chat;
import com.chatpulse.model.Message;
import com.chatpulse.model.User;
import com.chatpulse.repository.ChatRepository;
import com.chatpulse.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ChatRepository chatRepository;

    public Message sendMessage(Long chatId, User sender, String content, String messageType, String filePath) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            throw new RuntimeException("Chat not found");
        }
        
        Chat chat = chatOpt.get();
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "TEXT");
        message.setFilePath(filePath);
        
        return messageRepository.save(message);
    }

    public List<Message> getChatMessages(Long chatId) {
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            throw new RuntimeException("Chat not found");
        }
        
        return messageRepository.findAllByChatOrderByTimestamp(chatOpt.get());
    }
}

