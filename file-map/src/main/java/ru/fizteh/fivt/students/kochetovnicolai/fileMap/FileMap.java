package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Launcher;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.StringParser;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProvider;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProviderFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Component
@Lazy
public class FileMap {

    private HashMap<String, Executable> commands;

    @Autowired
    private static ApplicationContext context;

    @Bean
    public static DistributedTableProvider tableProvider() {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("fizteh.db.dir");
        if (property == null) {
            System.err.println("property fizteh.db.dir not found");
            System.exit(1);
        }

        DistributedTableProviderFactory factory = new DistributedTableProviderFactory();
        try {
            return factory.create(property);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private FileMap() {
        commands = new HashMap<>();
        Map<String, Executable> executableBeans = context.getBeansOfType(Executable.class);
        for (Executable executableBean : executableBeans.values())
            commands.put(executableBean.getName(), executableBean);
    }

    public static void main(String[] args) {
        context = AppConfig.context = new AnnotationConfigApplicationContext(AppConfig.class);
        FileMap fileHashMap = new FileMap();
        TableManager manager = context.getBean(TableManager.class);
        Launcher launcher = new Launcher(fileHashMap.commands, new StringParser() {
            @Override
            public String[] parse(String string) {
                String[] stringList = string.trim().split("[\\s]+");
                if (stringList.length < 2 || !stringList[0].equals("put")) {
                    return stringList;
                } else {
                    String[] newStringList = new String[3];
                    newStringList[0] = stringList[0];
                    newStringList[1] = stringList[1];
                    newStringList[2] = string.replaceFirst("[\\s]*put[\\s]+" + stringList[1] + "\\s", "");
                    return newStringList;
                }
            }
        });
        try {
            if (!launcher.launch(args, manager)) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
