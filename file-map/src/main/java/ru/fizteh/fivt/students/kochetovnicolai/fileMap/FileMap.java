package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Launcher;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.StringParser;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProvider;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProviderFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


@Component
@Lazy
public class FileMap {

    private HashMap<String, Executable> commands;

    @Autowired TableManager tableManager;

    @Autowired
    private FileMap(ApplicationContext context, List<Executable> executableBeans) {
        commands = new HashMap<>();
        for (Executable executableBean : executableBeans)
            commands.put(executableBean.getName(), executableBean);
    }

    @Bean
    @Lazy
    @Autowired
    public static DistributedTableProvider tableProvider(ApplicationContext context) {
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

    public void start(String args[]) {
        Launcher launcher = new Launcher(commands, new StringParser() {
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
            if (!launcher.launch(args, tableManager)) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
