package com.project.chatz.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.project.chatz.Payload.SendMessageRequest;
import com.project.chatz.exception.ChatException;
import com.project.chatz.exception.MessageException;
import com.project.chatz.exception.UserException;
import com.project.chatz.model.Chat;
import com.project.chatz.model.Message;
import com.project.chatz.model.User;
import com.project.chatz.repository.MessageRepository;
import com.project.chatz.service.MessageService;



@Service
public class MessageServiceImplementation implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserServiceImplementation userService;

    @Autowired
    private ChatServiceImplementation chatService;

     @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public Message sendMessage(SendMessageRequest req) throws UserException, ChatException {
        User user = this.userService.findUserById(req.getUserId());
        Chat chat = this.chatService.findChatById(req.getChatId());

        Message message = new Message();
        message.setChat(chat);
        message.setUser(user);
        message.setContent(req.getContent());
        message.setTimestamp(LocalDateTime.now());

        message = this.messageRepository.save(message);

        // Send message to WebSocket topic based on chat type
        if (chat.isGroup()) {
            messagingTemplate.convertAndSend("/group/" + chat.getId(), message);
        } else {
            messagingTemplate.convertAndSend( "/user/" + chat.getId(), message);
        }

        return message;
    }

    @Override
    public List<Message> getChatsMessages(Integer chatId, User reqUser) throws ChatException, UserException {

        Chat chat = this.chatService.findChatById(chatId);

        if (!chat.getUsers().contains(reqUser)) {
            throw new UserException("You are not related to this chat");
        }

        List<Message> messages = this.messageRepository.findByChatId(chat.getId());

        return messages;

    }

    @Override
    public Message findMessageById(Integer messageId) throws MessageException {
        Message message = this.messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("The required message is not found"));
        return message;
    }

    @Override
    public void deleteMessage(Integer messageId, User reqUser) throws MessageException {
        Message message = this.messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("The required message is not found"));

        if (message.getUser().getId() == reqUser.getId()) {
            this.messageRepository.delete(message);
        } else {
            throw new MessageException("You are not authorized for this task");
        }
    }

}