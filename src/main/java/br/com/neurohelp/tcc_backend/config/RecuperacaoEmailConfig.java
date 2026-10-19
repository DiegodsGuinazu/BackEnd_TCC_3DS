package br.com.neurohelp.tcc_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RecuperacaoEmailConfig {
    @Bean
    public ThreadPoolTaskExecutor recuperacaoEmailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); executor.setMaxPoolSize(2); executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("recuperacao-email-");
        return executor;
    }
}
