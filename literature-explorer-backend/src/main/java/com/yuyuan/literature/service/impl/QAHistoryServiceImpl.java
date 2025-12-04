package com.yuyuan.literature.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyuan.literature.dto.QARecordVO;
import com.yuyuan.literature.dto.QASessionVO;
import com.yuyuan.literature.dto.SaveQARequest;
import com.yuyuan.literature.entity.Literature;
import com.yuyuan.literature.entity.QAHistory;
import com.yuyuan.literature.mapper.QAHistoryMapper;
import com.yuyuan.literature.service.LiteratureService;
import com.yuyuan.literature.service.QAHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答历史服务实现
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAHistoryServiceImpl extends ServiceImpl<QAHistoryMapper, QAHistory> implements QAHistoryService {

    private final LiteratureService literatureService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveQA(SaveQARequest request) {
        String sessionId = request.getSessionId();

        // 新会话：生成会话ID
        if (StrUtil.isBlank(sessionId)) {
            sessionId = IdUtil.fastSimpleUUID();
        }

        // 查询当前会话已有的记录数（确定序号）
        int sequence = 1;
        Long count = this.lambdaQuery()
                .eq(QAHistory::getSessionId, sessionId)
                .count();
        if (count != null && count > 0) {
            sequence = count.intValue() + 1;
        }

        // 构建问答记录
        QAHistory history = new QAHistory();
        history.setSessionId(sessionId);
        history.setQuestion(request.getQuestion());
        history.setAnswer(request.getAnswer());
        history.setLiteratureId(request.getLiteratureId());
        history.setCrossDoc(request.getCrossDoc());
        history.setKeyword(request.getKeyword());
        history.setSequence(sequence);

        // 会话标题（第一条记录时设置）
        if (sequence == 1) {
            String title = StrUtil.isNotBlank(request.getSessionTitle())
                    ? request.getSessionTitle()
                    : truncateQuestion(request.getQuestion());
            history.setSessionTitle(title);
        } else {
            // 获取第一条记录的标题
            QAHistory firstRecord = this.lambdaQuery()
                    .eq(QAHistory::getSessionId, sessionId)
                    .eq(QAHistory::getSequence, 1)
                    .one();
            if (firstRecord != null) {
                history.setSessionTitle(firstRecord.getSessionTitle());
            }
        }

        this.save(history);
        log.info("保存问答记录成功，会话ID: {}, 序号: {}", sessionId, sequence);

        return sessionId;
    }

    @Override
    public List<QASessionVO> listSessions(Long literatureId) {
        // 查询会话ID列表
        List<String> sessionIds = this.baseMapper.selectSessionIds(literatureId);

        List<QASessionVO> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            QASessionVO session = buildSessionVO(sessionId);
            if (session != null) {
                sessions.add(session);
            }
        }

        return sessions;
    }

    @Override
    public QASessionVO getSessionDetail(String sessionId) {
        return buildSessionVO(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        this.baseMapper.deleteBySessionId(sessionId);
        log.info("删除会话成功，会话ID: {}", sessionId);
    }

    /**
     * 构建会话VO
     */
    private QASessionVO buildSessionVO(String sessionId) {
        List<QAHistory> histories = this.baseMapper.selectBySessionId(sessionId);
        if (histories == null || histories.isEmpty()) {
            return null;
        }

        QAHistory firstRecord = histories.get(0);

        QASessionVO session = new QASessionVO();
        session.setSessionId(sessionId);
        session.setSessionTitle(firstRecord.getSessionTitle());
        session.setLiteratureId(firstRecord.getLiteratureId());
        session.setCrossDoc(firstRecord.getCrossDoc());
        session.setQaCount(histories.size());
        session.setCreateTime(firstRecord.getCreateTime());
        session.setUpdateTime(histories.get(histories.size() - 1).getUpdateTime());

        // 获取文献名称
        if (firstRecord.getLiteratureId() != null) {
            Literature literature = literatureService.getById(firstRecord.getLiteratureId());
            if (literature != null) {
                session.setLiteratureName(literature.getOriginalName());
            }
        }

        // 转换问答记录
        List<QARecordVO> records = histories.stream().map(h -> {
            QARecordVO record = new QARecordVO();
            record.setId(h.getId());
            record.setQuestion(h.getQuestion());
            record.setAnswer(h.getAnswer());
            record.setSequence(h.getSequence());
            record.setCreateTime(h.getCreateTime());
            return record;
        }).collect(Collectors.toList());

        session.setRecords(records);

        return session;
    }

    /**
     * 截断问题作为标题
     */
    private String truncateQuestion(String question) {
        if (StrUtil.isBlank(question)) {
            return "未命名对话";
        }
        if (question.length() <= 30) {
            return question;
        }
        return question.substring(0, 30) + "...";
    }
}
