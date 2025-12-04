package com.yuyuan.literature.controller;

import com.yuyuan.literature.common.result.Result;
import com.yuyuan.literature.dto.QASessionVO;
import com.yuyuan.literature.dto.SaveQARequest;
import com.yuyuan.literature.service.QAHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答历史控制器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/qa-history")
@RequiredArgsConstructor
@Tag(name = "问答历史管理", description = "问答历史记录相关接口")
public class QAHistoryController {

    private final QAHistoryService qaHistoryService;

    @PostMapping("/save")
    @Operation(summary = "保存问答记录", description = "保存用户的问答记录到历史")
    public Result<String> saveQA(
            @Parameter(description = "保存请求", required = true) @Validated @RequestBody SaveQARequest request) {

        log.info("保存问答记录，问题: {}", request.getQuestion());
        String sessionId = qaHistoryService.saveQA(request);
        return Result.success(sessionId);
    }

    @GetMapping("/sessions")
    @Operation(summary = "查询会话列表", description = "查询所有或指定文献的问答会话列表")
    public Result<List<QASessionVO>> listSessions(
            @Parameter(description = "文献ID（可选）") @RequestParam(required = false) Long literatureId) {

        log.info("查询会话列表，文献ID: {}", literatureId);
        List<QASessionVO> sessions = qaHistoryService.listSessions(literatureId);
        return Result.success(sessions);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "查询会话详情", description = "查询指定会话的完整问答记录")
    public Result<QASessionVO> getSessionDetail(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {

        log.info("查询会话详情，会话ID: {}", sessionId);
        QASessionVO session = qaHistoryService.getSessionDetail(sessionId);
        return Result.success(session);
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的问答会话及其所有记录")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {

        log.info("删除会话，会话ID: {}", sessionId);
        qaHistoryService.deleteSession(sessionId);
        return Result.success();
    }
}
