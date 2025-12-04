package com.yuyuan.literature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuyuan.literature.entity.QAHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 问答历史 Mapper 接口
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Mapper
public interface QAHistoryMapper extends BaseMapper<QAHistory> {

    /**
     * 查询会话列表（按文献分组）
     *
     * @param literatureId 文献ID（可选）
     * @return 会话列表
     */
    List<String> selectSessionIds(@Param("literatureId") Long literatureId);

    /**
     * 查询会话详情
     *
     * @param sessionId 会话ID
     * @return 问答记录列表
     */
    List<QAHistory> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
