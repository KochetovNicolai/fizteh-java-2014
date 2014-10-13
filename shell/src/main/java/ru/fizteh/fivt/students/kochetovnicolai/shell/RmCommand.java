package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import java.io.File;

@Component
@Lazy
public class RmCommand extends Executable {

    @Autowired
    private FileManager manager;

    public RmCommand() {
        super("rm", 2);
    }

    @Override
    public boolean execute(String[] args) {
        File file = manager.resolvePath(args[1]);
        if (file == null) {
            manager.printMessage(args[0] + ": cannot remove \'" + args[1] + "\': No such file or directory");
            return false;
        }
        if (file.isDirectory() && manager.getCurrentPath().getAbsolutePath().contains(file.getAbsolutePath())) {
            manager.printMessage(args[0] + ": cannot remove \'" + args[1] + "\': cannot delete current directory");
            return false;
        }
        return manager.recursiveRemove(file, args[0]);
    }
}
