package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class PwdCommand extends Executable {

    @Autowired
    private FileManager manager;

    public PwdCommand() {
        super("pwd", 1);
    }

    @Override
    public boolean execute(String[] args) {
        manager.printMessage(manager.getCurrentPath().getAbsolutePath());
        return true;
    }
}
