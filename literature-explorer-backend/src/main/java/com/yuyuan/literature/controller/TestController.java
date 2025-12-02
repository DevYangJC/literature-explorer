package com.yuyuan.literature.controller;

import com.yuyuan.literature.common.exception.BusinessException;
import com.yuyuan.literature.common.exception.ValidationException;
import com.yuyuan.literature.common.request.PageRequest;
import com.yuyuan.literature.common.request.PageResult;
import com.yuyuan.literature.common.result.Result;
import com.yuyuan.literature.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 测试控制器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/test")
@Tag(name = "测试接口", description = "用于测试各种功能的接口")
@Validated
public class TestController {

    /**
     * 简单测试接口
     */
    @GetMapping("/hello")
    @Operation(summary = "Hello 接口", description = "简单的问候接口")
    public Result<String> hello(@Parameter(description = "姓名") @RequestParam(defaultValue = "World") String name) {
        log.info("Hello 接口被调用，参数: {}", name);
        String message = StrUtil.format("Hello, {}! 当前时间: {}", name, DateUtil.formatDateTime(new Date()));
        return Result.success(message);
    }

    /**
     * 测试分页接口
     */
    @PostMapping("/page")
    @Operation(summary = "分页测试接口", description = "测试分页功能")
    public Result<PageResult<TestData>> testPage(@Valid @RequestBody PageRequest pageRequest) {
        log.info("分页测试接口被调用，参数: {}", pageRequest);
        
        // 模拟数据
        List<TestData> records = new ArrayList<>();
        for (int i = 1; i <= pageRequest.getPageSize(); i++) {
            TestData data = new TestData();
            data.setId((long) ((pageRequest.getPageNum() - 1) * pageRequest.getPageSize() + i));
            data.setName("测试数据 " + data.getId());
            data.setDescription("这是第 " + data.getId() + " 条测试数据");
            data.setCreateTime(new Date());
            records.add(data);
        }
        
        // 模拟总数
        long total = 100L;
        
        PageResult<TestData> pageResult = PageResult.of(records, total, pageRequest);
        return Result.success(pageResult);
    }

    /**
     * 测试参数校验
     */
    @PostMapping("/validate")
    @Operation(summary = "参数校验测试", description = "测试参数校验功能")
    public Result<String> testValidate(@Valid @RequestBody TestRequest request) {
        log.info("参数校验测试接口被调用，参数: {}", request);
        return Result.success("参数校验通过！");
    }

    /**
     * 测试业务异常
     */
    @GetMapping("/business-error")
    @Operation(summary = "业务异常测试", description = "测试业务异常处理")
    public Result<Void> testBusinessError() {
        log.info("业务异常测试接口被调用");
        throw new BusinessException("这是一个业务异常测试");
    }

    /**
     * 测试参数校验异常
     */
    @GetMapping("/validation-error")
    @Operation(summary = "校验异常测试", description = "测试校验异常处理")
    public Result<Void> testValidationError() {
        log.info("校验异常测试接口被调用");
        throw new ValidationException("这是一个参数校验异常测试");
    }

    /**
     * 测试系统异常
     */
    @GetMapping("/system-error")
    @Operation(summary = "系统异常测试", description = "测试系统异常处理")
    public Result<Void> testSystemError() {
        log.info("系统异常测试接口被调用");
        throw new RuntimeException("这是一个系统异常测试");
    }

    /**
     * 测试自定义错误码
     */
    @GetMapping("/custom-error")
    @Operation(summary = "自定义错误码测试", description = "测试自定义错误码")
    public Result<Void> testCustomError() {
        log.info("自定义错误码测试接口被调用");
        throw new BusinessException(ResultCode.DATA_NOT_EXIST);
    }

    /**
     * 测试路径参数校验
     */
    @GetMapping("/path/{id}")
    @Operation(summary = "路径参数校验测试", description = "测试路径参数校验")
    public Result<String> testPathValidation(
            @Parameter(description = "ID，必须不为空") 
            @PathVariable @NotNull(message = "ID不能为空") Long id) {
        log.info("路径参数校验测试接口被调用，ID: {}", id);
        return Result.success("路径参数校验通过，ID: " + id);
    }

    /**
     * 测试请求参数校验
     */
    @GetMapping("/param")
    @Operation(summary = "请求参数校验测试", description = "测试请求参数校验")
    public Result<String> testParamValidation(
            @Parameter(description = "名称，必须不为空") 
            @RequestParam @NotBlank(message = "名称不能为空") String name) {
        log.info("请求参数校验测试接口被调用，名称: {}", name);
        return Result.success("请求参数校验通过，名称: " + name);
    }

    /**
     * 测试数据类
     */
    @Data
    public static class TestData {
        private Long id;
        private String name;
        private String description;
        private Date createTime;
    }

    /**
     * 测试请求类
     */
    @Data
    public static class TestRequest {
        
        @NotBlank(message = "名称不能为空")
        private String name;
        
        @NotNull(message = "年龄不能为空")
        private Integer age;
        
        private String description;
    }
}
