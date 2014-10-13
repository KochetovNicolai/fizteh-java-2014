package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;


@Component
@Lazy
public class DirCommand extends Executable {

    @Autowired
    private FileManager manager;

    public DirCommand() {
        super("dir", 1);
    }

    @Override
    public boolean execute(String[] args) {
        String[] directories = manager.getCurrentPath().list();
        if (directories != null) {
            for (String directory : directories) {
                System.out.println(directory);
            }
        }
        return true;
    }
}
