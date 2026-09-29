package com.thejoa703.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig  implements WebMvcConfigurer{
	// application.yml  에서 업로드된 경로불러오기
	@Value("${file.upload-dir}")
	private String uploadDir;
	
	// 이미지리소스
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")  //  /uploads 호출경로
				.addResourceLocations("file:" + uploadDir + "/");  // 실제올리는경로
	}
}
