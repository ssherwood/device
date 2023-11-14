package com.example.demo.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.stereotype.Component;

@EnableRetry
@Component
public class RetryInterceptor {

    @Bean("ysqlRetryInterceptor")
    public RetryOperationsInterceptor ysqlRetryInterceptor(RetryPolicy retryPolicy, BackOffPolicy backOffPolicy) {
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(retryPolicy)
                .backOffPolicy(backOffPolicy)
                .build();
    }
}