package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan("ru.fizteh.fivt.students.kochetovnicolai.fileMap")
@PropertySource("classpath:config.cfg")
public class AppConfig {

    public static ApplicationContext context;

    @Bean
    public static ApplicationContext getAppContext() {
        return context;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholder() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
