// Kochetov Nicolai, 294, Shell

package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Launcher;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.StringParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Shell {

    private HashMap<String, Executable> commands;

    public Shell(ApplicationContext context) {
        commands = new HashMap<>();
        Map<String, Executable> executableBeans = context.getBeansOfType(Executable.class);
        for (Executable executableBean : executableBeans.values())
            commands.put(executableBean.getName(), executableBean);
    }

    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Shell shell = new Shell(context);
        Launcher launcher = new Launcher(shell.commands, new StringParser() {
            @Override
            public String[] parse(String string) {
                return string.trim().split("[\\s]+");
            }
        });
        try {
            if (!launcher.launch(args, context.getBean(FileManager.class))) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
