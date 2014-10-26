package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Launcher;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.StringParser;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProvider;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProviderFactory;

import java.io.IOException;

@Configuration
@ComponentScan("ru.fizteh.fivt.students.kochetovnicolai.fileMap")
@PropertySource("classpath:config.properties")
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



    public static void main(String args[]) {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        FileMap fileHashMap = context.getBean(FileMap.class);
        fileHashMap.start(args);
    }
}
