package com.yuyuan.literature.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyuan.literature.common.request.PageResult;
import com.yuyuan.literature.dto.BatchLiteratureImportRequest;
import com.yuyuan.literature.dto.LiteratureQueryRequest;
import com.yuyuan.literature.dto.LiteratureVO;
import com.yuyuan.literature.entity.Literature;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文献服务接口
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
public interface LiteratureService extends IService<Literature> {

    /**
     * 创建文献记录
     *
     * @param file 上传的文件
     * @param filePath 文件存储路径
     * @param contentLength 内容长度
     * @return 文献ID
     */
    Long createLiterature(MultipartFile file, String filePath, Integer contentLength);

    /**
     * 更新阅读指南
     *
     * @param id 文献ID
     * @param readingGuide 阅读指南内容
     */
    void updateReadingGuide(Long id, String readingGuide);

    /**
     * 更新分类和描述
     *
     * @param id 文献ID
     * @param tags 分类标签
     * @param description 描述
     */
    void updateClassification(Long id, List<String> tags, String description);

    /**
     * 更新状态
     *
     * @param id 文献ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 分页查询文献
     *
     * @param request 查询条件
     * @return 分页结果
     */
    PageResult<LiteratureVO> pageLiteratures(LiteratureQueryRequest request);

    /**
     * 根据ID获取文献详情
     *
     * @param id 文献ID
     * @return 文献详情
     */
    LiteratureVO getLiteratureDetail(Long id);

    /**
     * 根据ID下载文献文件
     *
     * @param id 文献ID
     * @param response HTTP响应对象
     */
    void downloadLiteratureFile(Long id, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 批量导入文献
     *
     * @param request 批量导入请求
     * @return SSE发射器
     */
    SseEmitter batchImportLiterature(BatchLiteratureImportRequest request);

    /**
     * 导出文献阅读指南为Markdown
     *
     * @param id 文献ID
     * @param response HTTP响应对象
     */
    void exportReadingGuideMarkdown(Long id, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 删除文献（逻辑删除）
     *
     * @param id 文献ID
     */
    void deleteLiterature(Long id);

    /**
     * 批量删除文献（逻辑删除）
     *
     * @param ids 文献ID列表
     */
    void batchDeleteLiterature(List<Long> ids);
}
