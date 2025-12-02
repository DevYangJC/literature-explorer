package com.yuyuan.literature.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j 配置类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * 创建 OpenAPI 实例
     */
    @Bean
    public OpenAPI createRestApi() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("本地开发环境"),
                        new Server().url("https://api.example.com").description("生产环境")
                ));
    }

    /**
     * API 信息
     */
    private Info apiInfo() {
        return new Info()
                .title("Literature Assistant API")
                .description("文学助手系统接口文档")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Literature Assistant Team")
                        .email("developer@example.com")
                        .url("https://github.com/example/literature-assistant"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}
