package com.yuyuan.literature.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuyuan.literature.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 响应工具类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
public class ResponseUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 私有构造方法
     */
    private ResponseUtils() {
    }

    /**
     * 向响应写入 JSON 数据
     *
     * @param response HTTP 响应
     * @param result   结果对象
     */
    public static void writeJson(HttpServletResponse response, Result<?> result) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        try {
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }

    /**
     * 向响应写入 JSON 数据，指定状态码
     *
     * @param response   HTTP 响应
     * @param result     结果对象
     * @param statusCode HTTP 状态码
     */
    public static void writeJson(HttpServletResponse response, Result<?> result, int statusCode) {
        response.setStatus(statusCode);
        writeJson(response, result);
    }
}
