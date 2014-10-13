package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class TableCommandExit extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        manager.setExit();
        return true;
    }

    public TableCommandExit() {
        super("exit", 1);
    }
}
