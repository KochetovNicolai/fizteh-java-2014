package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;
import java.io.File;


@Component
@Lazy
public class CpCommand extends Executable {

    @Autowired
    private FileManager manager;

    public CpCommand() {
        super("cp", 3);
    }

    @Override
    public boolean execute(String[] args) {
        File source = manager.resolvePath(args[1]);
        File destination = manager.resolvePath(args[2]);
        return manager.safeCopy(source, destination, getName());
    }
}
