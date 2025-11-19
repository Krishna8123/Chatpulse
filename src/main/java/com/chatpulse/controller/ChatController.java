package com.chatpulse.controller;

import com.chatpulse.model.*;
import com.chatpulse.service.ChatService;
import com.chatpulse.service.MessageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
public class ChatController {
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private MessageService messageService;
    
    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping("/chat")
    public String chatPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        
        // Get pending requests
        List<ChatRequest> pendingRequests = chatService.getPendingRequests(user);
        model.addAttribute("pendingRequests", pendingRequests);
        
        // Get private chats and set display names
        List<Chat> privateChats = chatService.getPrivateChats(user);
        // For private chats, set display name to the other user's username
        for (Chat chat : privateChats) {
            if ("PRIVATE".equals(chat.getType())) {
                if (chat.getCreatedBy().getId().equals(user.getId())) {
                    // Current user created it, name already shows the other person
                    // Keep as is
                } else {
                    // Other person created it, show their username
                    chat.setName(chat.getCreatedBy().getUsername());
                }
            }
        }
        model.addAttribute("privateChats", privateChats);
        
        // Get group chats
        List<Chat> groupChats = chatService.getGroupChats(user);
        model.addAttribute("groupChats", groupChats);
        
        return "chat";
    }

    @PostMapping("/chat/create-private")
    @ResponseBody
    public ResponseEntity<?> createPrivateChat(@RequestParam String phoneNumber,
                                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            chatService.createPrivateChatRequest(user, phoneNumber);
            return ResponseEntity.ok().body("Request sent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/chat/create-group")
    @ResponseBody
    public ResponseEntity<?> createGroupChat(@RequestParam String groupName,
                                            @RequestParam String phoneNumbers,
                                            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            List<String> memberPhones = List.of(phoneNumbers.split(","));
            chatService.createGroupChatRequest(user, groupName, memberPhones);
            return ResponseEntity.ok().body("Group created and requests sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/chat/accept-request/{requestId}")
    @ResponseBody
    public ResponseEntity<?> acceptRequest(@PathVariable Long requestId,
                                          @RequestParam String type,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            if ("PRIVATE".equals(type)) {
                chatService.acceptPrivateChatRequest(requestId, user);
            } else if ("GROUP".equals(type)) {
                chatService.acceptGroupChatRequest(requestId, user);
            }
            return ResponseEntity.ok().body("Request accepted");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/chat/reject-request/{requestId}")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            chatService.rejectChatRequest(requestId, user);
            return ResponseEntity.ok().body("Request rejected");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/chat/messages/{chatId}")
    @ResponseBody
    public ResponseEntity<?> getMessages(@PathVariable Long chatId,
                                         HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            List<Message> messages = messageService.getChatMessages(chatId);
            return ResponseEntity.ok().body(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/chat/send-message")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestParam Long chatId,
                                        @RequestParam(required = false) String content,
                                        @RequestParam(required = false) MultipartFile file,
                                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            String messageType = "TEXT";
            String filePath = null;
            
            if (file != null && !file.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePathObj = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePathObj);
                filePath = UPLOAD_DIR + fileName;
                
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("image/")) {
                    messageType = "IMAGE";
                } else {
                    messageType = "FILE";
                }
            }
            
            Message message = messageService.sendMessage(chatId, user, content, messageType, filePath);
            return ResponseEntity.ok().body(message);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("File upload failed");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

