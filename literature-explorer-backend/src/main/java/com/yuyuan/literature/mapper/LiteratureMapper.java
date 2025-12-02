package com.yuyuan.literature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyuan.literature.dto.LiteratureQueryRequest;
import com.yuyuan.literature.entity.Literature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
