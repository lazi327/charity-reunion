package com.charity.reunion.config;

import com.charity.reunion.security.TokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;

/**
 * Web 配置:跨域 / 拦截器 / 上传文件静态映射 / 前端 SPA 路由回退
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;
    private final String uploadDir;

    public WebConfig(TokenInterceptor tokenInterceptor,
                     @Value("${dating.upload-dir:./upload}") String uploadDir) {
        this.tokenInterceptor = tokenInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = new File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + absolute + File.separator);

        // 前端 dist 打进 jar 后,深链/刷新要回 index.html 交给 vue-router;接口与图片路径不参与回退
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("upload/")) {
                            return null;
                        }
                        Resource requested = location.createRelative(resourcePath);
                        return requested.exists() && requested.isReadable()
                                ? requested : new ClassPathResource("static/index.html");
                    }
                });
    }
}
