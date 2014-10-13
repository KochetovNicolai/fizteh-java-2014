package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import java.io.File;

@Component
@Lazy
public class MkdirCommand extends Executable {

    @Autowired
    private FileManager manager;

    public MkdirCommand() {
        super("mkdir", 2);
    }

    @Override
    public boolean execute(String[] args) {
        try {
            File newDirectory = new File(manager.getCurrentPath().getAbsolutePath() + File.separator + args[1]);
            if (newDirectory.exists()) {
                manager.printMessage(args[0] + ": \'" + args[1] + "\': directory already exists");
                return false;
            } else if (!newDirectory.mkdir()) {
                manager.printMessage(args[0] + ": \'" + args[1] + "\': couldn't create directory");
            }
            return true;
        } catch (SecurityException e) {
            manager.printMessage(args[0] + ": \'" + args[1] + "\': couldn't create directory");
        }
        return false;
    }
}
