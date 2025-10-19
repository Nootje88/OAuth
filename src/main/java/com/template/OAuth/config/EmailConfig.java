package com.template.OAuth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import java.util.Set;


import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class EmailConfig {


    // Thymeleaf template resolver for email templates (classpath:/templates/mail/*.html)
    @Bean
    public SpringResourceTemplateResolver emailTemplateResolver() {
        SpringResourceTemplateResolver r = new SpringResourceTemplateResolver();
        r.setPrefix("classpath:/templates/");     // keep this generic
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding(StandardCharsets.UTF_8.name());
        r.setCheckExistence(true);

        // Only resolve templates under "mail/*" so it won't clash with web views
        r.setResolvablePatterns(Set.of("mail/*"));
        r.setOrder(1); // higher priority than default (which is usually 10)
        return r;
}

    
}
