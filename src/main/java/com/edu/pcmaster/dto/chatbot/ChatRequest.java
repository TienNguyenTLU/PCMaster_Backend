package com.edu.pcmaster.dto.chatbot;

import java.util.List;


public record ChatRequest(
    String message,
    List<ChatMessageDto> history,
    String mode
) {}
