package com.yuyuan.literature.common.constant;

/**
 * 通用常量
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
public class CommonConstants {

    /**
     * 私有构造方法
     */
    private CommonConstants() {
    }

    /**
     * 成功状态
     */
    public static final String SUCCESS = "success";

    /**
     * 失败状态
     */
    public static final String FAIL = "fail";

    /**
     * 错误状态
     */
    public static final String ERROR = "error";

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 升序
     */
    public static final String ASC = "ASC";

    /**
     * 降序
     */
    public static final String DESC = "DESC";

    /**
     * 默认排序字段
     */
    public static final String DEFAULT_SORT_FIELD = "createTime";

    /**
     * UTF-8 编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 日期时间格式
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 请求头 - 请求ID
     */
    public static final String HEADER_REQUEST_ID = "X-Request-ID";

    /**
     * 请求头 - 客户端版本
     */
    public static final String HEADER_CLIENT_VERSION = "X-Client-Version";

    /**
     * 请求头 - 设备类型
     */
    public static final String HEADER_DEVICE_TYPE = "X-Device-Type";

    /**
     * 请求头 - 用户代理
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * 请求头 - 授权
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * 请求头 - 内容类型
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 设备类型 - Web
     */
    public static final String DEVICE_TYPE_WEB = "WEB";

    /**
     * 设备类型 - 移动端
     */
    public static final String DEVICE_TYPE_MOBILE = "MOBILE";

    /**
     * 设备类型 - 桌面端
     */
    public static final String DEVICE_TYPE_DESKTOP = "DESKTOP";

    /**
     * 删除标记 - 未删除
     */
    public static final Integer DELETE_FLAG_NORMAL = 0;

    /**
     * 删除标记 - 已删除
     */
    public static final Integer DELETE_FLAG_DELETED = 1;

    /**
     * 状态 - 启用
     */
    public static final Integer STATUS_ENABLED = 1;

    /**
     * 状态 - 禁用
     */
    public static final Integer STATUS_DISABLED = 0;
}
