package com.yuyuan.literature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyuan.literature.dto.QASessionVO;
import com.yuyuan.literature.dto.SaveQARequest;
import com.yuyuan.literature.entity.QAHistory;

import java.util.List;

/**
 * 问答历史服务接口
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
public interface QAHistoryService extends IService<QAHistory> {

    /**
     * 保存问答记录
     *
     * @param request 保存请求
     * @return 会话ID
     */
    String saveQA(SaveQARequest request);

    /**
     * 查询会话列表
     *
     * @param literatureId 文献ID（可选）
     * @return 会话列表
     */
    List<QASessionVO> listSessions(Long literatureId);

    /**
     * 查询会话详情
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    QASessionVO getSessionDetail(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);
}
