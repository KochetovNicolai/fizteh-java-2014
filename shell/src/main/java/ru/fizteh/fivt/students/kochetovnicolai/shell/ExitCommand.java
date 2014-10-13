package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;


@Component
@Lazy
public class ExitCommand extends Executable {

    @Autowired
    private FileManager manager;

    public ExitCommand() {
        super("exit", 1);
    }

    @Override
    public boolean execute(String[] args) {
        manager.setExit();
        return true;
    }
}
