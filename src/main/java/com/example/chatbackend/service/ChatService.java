package com.example.chatbackend.service;

import com.example.chatbackend.config.OpenRouterProperties;
import com.example.chatbackend.domain.ChatMessage;
import com.example.chatbackend.domain.Conversation;
import com.example.chatbackend.domain.MessageRole;
import com.example.chatbackend.domain.UserAccount;
import com.example.chatbackend.dto.ChatRequest;
import com.example.chatbackend.dto.ChatResponse;
import com.example.chatbackend.dto.ConversationResponse;
import com.example.chatbackend.dto.MessageResponse;
import com.example.chatbackend.dto.PageResponse;
import com.example.chatbackend.exception.ChatNotFoundException;
import com.example.chatbackend.openrouter.OpenRouterClient;
import com.example.chatbackend.openrouter.OpenRouterMessage;
import com.example.chatbackend.repository.ConversationRepository;
import com.example.chatbackend.repository.MessageRepository;
import com.example.chatbackend.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final OpenRouterClient openRouterClient;
    private final OpenRouterProperties openRouterProperties;
    private final RateLimitService rateLimitService;
    private final ChatMapper mapper;

    public ChatService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            OpenRouterClient openRouterClient,
            OpenRouterProperties openRouterProperties,
            RateLimitService rateLimitService,
            ChatMapper mapper
    ) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.openRouterClient = openRouterClient;
        this.openRouterProperties = openRouterProperties;
        this.rateLimitService = rateLimitService;
        this.mapper = mapper;
    }

    public ChatResponse sendMessage(ChatRequest request) {
        rateLimitService.checkAllowed(request.userId());

        UserAccount user = getOrCreateUser(request.userId());
        Conversation conversation = getOrCreateConversation(user, request.conversationId(), request.title());
        String model = StringUtils.hasText(request.model()) ? request.model() : openRouterProperties.getDefaultModel();

        ChatMessage userMessage = messageRepository.save(
                new ChatMessage(conversation, MessageRole.USER, request.prompt().trim(), model)
        );

        List<OpenRouterMessage> context = buildContext(conversation.getId(), request.prompt());
        OpenRouterMessage aiMessage = openRouterClient.complete(model, context);

        ChatMessage assistantMessage = messageRepository.save(
                new ChatMessage(conversation, MessageRole.ASSISTANT, aiMessage.content(), model)
        );
        conversation.touch();
        conversationRepository.save(conversation);

        return new ChatResponse(
                conversation.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                MessageRole.ASSISTANT.name().toLowerCase(),
                assistantMessage.getContent(),
                model,
                assistantMessage.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listConversations(String userId, Pageable pageable) {
        Page<ConversationResponse> page = conversationRepository
                .findByUserExternalUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(mapper::toConversationResponse);
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(String userId, UUID conversationId, Pageable pageable) {
        Conversation conversation = findConversationForUser(conversationId, userId);
        Page<MessageResponse> page = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId(), pageable)
                .map(mapper::toMessageResponse);
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
    }

    @Transactional
    public void deleteConversation(String userId, UUID conversationId) {
        Conversation conversation = findConversationForUser(conversationId, userId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    private UserAccount getOrCreateUser(String externalUserId) {
        return userRepository.findByExternalUserId(externalUserId)
                .orElseGet(() -> userRepository.save(new UserAccount(externalUserId)));
    }

    private Conversation getOrCreateConversation(UserAccount user, UUID conversationId, String title) {
        if (conversationId == null) {
            String conversationTitle = StringUtils.hasText(title) ? title.trim() : "New conversation";
            return conversationRepository.save(new Conversation(user, conversationTitle));
        }
        return findConversationForUser(conversationId, user.getExternalUserId());
    }

    private Conversation findConversationForUser(UUID conversationId, String userId) {
        return conversationRepository.findByIdAndUserExternalUserId(conversationId, userId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found"));
    }

    private List<OpenRouterMessage> buildContext(UUID conversationId, String currentPrompt) {
        List<ChatMessage> recentMessages = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
        List<OpenRouterMessage> messages = new ArrayList<>(recentMessages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(this::toOpenRouterMessage)
                .toList());

        if (messages.stream().noneMatch(message -> currentPrompt.equals(message.content()))) {
            messages.add(new OpenRouterMessage("user", currentPrompt));
        }

        int maxContextMessages = openRouterProperties.getMaxContextMessages();
        if (messages.size() > maxContextMessages) {
            return messages.subList(messages.size() - maxContextMessages, messages.size());
        }
        return messages;
    }

    private OpenRouterMessage toOpenRouterMessage(ChatMessage message) {
        return new OpenRouterMessage(message.getRole().name().toLowerCase(), message.getContent());
    }

    public static Pageable boundedPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }
}
