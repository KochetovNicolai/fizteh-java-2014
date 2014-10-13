package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import java.io.File;

@Component
@Lazy
public class CdCommand extends Executable {

    @Autowired
    private FileManager manager;

    public CdCommand() {
        super("cd", 2);
    }

    @Override
    public boolean execute(String[] args) {
        File newPath = manager.resolvePath(args[1]);
        if (newPath == null || !newPath.exists()) {
            manager.printMessage(args[0] + ": \'" + args[1] + "\': No such file or directory");
        } else if (!newPath.isDirectory()) {
            manager.printMessage(args[0] + ": \'" + args[1] + "': expected directory name, but file found");
        } else {
            return manager.setCurrentPath(newPath);
        }
        return false;
    }
}
