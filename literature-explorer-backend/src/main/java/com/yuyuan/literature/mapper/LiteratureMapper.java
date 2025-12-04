package com.yuyuan.literature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyuan.literature.dto.LiteratureContext;
import com.yuyuan.literature.dto.LiteratureQueryRequest;
import com.yuyuan.literature.entity.Literature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文献 Mapper 接口
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Mapper
public interface LiteratureMapper extends BaseMapper<Literature> {

    /**
     * 分页查询文献
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<Literature> selectLiteraturePage(Page<Literature> page, @Param("req") LiteratureQueryRequest request);

    /**
     * 检索相关文献（用于问答系统）
     *
     * @param keyword 关键词
     * @param topK    返回数量
     * @return 相关文献上下文列表
     */
    List<LiteratureContext> searchRelevantLiteratures(@Param("keyword") String keyword, @Param("topK") Integer topK);
}
