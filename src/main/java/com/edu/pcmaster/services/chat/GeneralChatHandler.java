package com.edu.pcmaster.services.chat;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler cho câu hỏi chung: chào hỏi, hỏi về cửa hàng, FAQ.
 * Không cần vector search, trả lời nhanh.
 */
@Component
public class GeneralChatHandler implements ChatHandler {

    private final ChatModel chatModel;

    private static final int MAX_HISTORY_TURNS = 4;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý ảo thân thiện của cửa hàng linh kiện máy tính PCMaster.

            THÔNG TIN CỬA HÀNG:
            - Tên: PCMaster — Chuyên linh kiện PC, laptop và phụ kiện gaming
            - Dịch vụ: Bán linh kiện, tư vấn build PC, kiểm tra tương thích linh kiện, phân tích bottleneck CPU/GPU
            - Sản phẩm: CPU, GPU/VGA, Mainboard, RAM, SSD/HDD, PSU, Case, Tản nhiệt, và nhiều hơn nữa

            PHONG CÁCH GIAO TIẾP:
            - Xưng "mình", gọi khách là "bạn"
            - Thân thiện, vui vẻ, dùng emoji phù hợp (😊👋🎉💻🎮)
            - Trả lời ngắn gọn, đi vào trọng tâm
            - Luôn gợi ý khách hỏi thêm nếu cần

            QUY TẮC:
            1. Nếu khách chào hỏi → chào lại thân thiện, giới thiệu ngắn về cửa hàng.
            2. Nếu khách hỏi về cửa hàng → cung cấp thông tin.
            3. Nếu khách hỏi mơ hồ liên quan đến sản phẩm → gợi ý hỏi cụ thể hơn.
               Ví dụ: "Bạn có thể cho mình biết thêm về ngân sách và mục đích sử dụng không? 😊"
            4. KHÔNG bịa thông tin sản phẩm, giá cả, tồn kho.
            5. Trả lời trong 2-4 câu là đủ.
            6. BẮT BUỘC trả lời hoàn toàn bằng tiếng Việt, tuyệt đối không dùng ngôn ngữ khác (trừ thuật ngữ chuyên ngành).
            """;

    public GeneralChatHandler(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public ChatResponse handle(String message, List<ChatMessageDto> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // Add conversation history (limited)
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            boolean firstUserFound = false;
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDto msg = history.get(i);
                if ("assistant".equalsIgnoreCase(msg.role()) && !firstUserFound) continue;
                if ("user".equalsIgnoreCase(msg.role())) {
                    firstUserFound = true;
                    messages.add(new UserMessage(msg.content()));
                } else if ("assistant".equalsIgnoreCase(msg.role())) {
                    messages.add(new AssistantMessage(msg.content()));
                }
            }
        }

        messages.add(new UserMessage(message));

        String aiResponse;
        try {
            Prompt prompt = new Prompt(messages);
            aiResponse = chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            System.err.println("[GeneralChat] LLM call failed: " + e.getMessage());
            aiResponse = "Chào bạn! 👋 Mình là trợ lý của PCMaster. Hiện tại mình đang gặp chút sự cố, bạn thử hỏi lại sau nhé! 🙏";
        }

        // General chat never has recommended products
        return new ChatResponse(aiResponse, List.of());
    }
}
