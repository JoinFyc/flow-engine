package com.wflow.config;

import com.wflow.bean.FlowProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * @author JoinFyc
 * @version : 1.0
 * @description 远程配置
 * @date 2024 09:09
 */
@Configuration
@Slf4j
public class RemoteConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 添加 Jackson 消息转换器
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        // 添加拦截器
        restTemplate.getInterceptors().add((request, body, execution) -> {
            final FlowProcessContext flowProcessContext = FlowProcessContext.getFlowProcessContext();
            // 在这里添加自定义逻辑，例如：设置请求头
            request.getHeaders().add("Authorization", "Bearer token");
            //租户ID
            request.getHeaders().add("tenantId", flowProcessContext.getTenantId());
            //用户ID
            request.getHeaders().add("userId", flowProcessContext.getUserId());
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
