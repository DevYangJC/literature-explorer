package com.yuyuan.literature.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kimi AI 聊天请求 DTO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
public class KimiChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 最大token数
     */
    @Alias("max_tokens")
    private Integer maxTokens;

    /**
     * 最大token数
     */
    @Alias("response_format")
    private ResponseFormat responseFormat;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 是否流式响应
     */
    private Boolean stream;

    @Builder
    @Data
    public static class ResponseFormat {
        private String type;
    }

    /**
     * 消息对象
     */
    @Data
    public static class Message {
        /**
         * 角色：system、user、assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
