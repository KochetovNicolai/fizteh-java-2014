package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class TableCommandStopHttp extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        return manager.stopHTTP();
    }

    public TableCommandStopHttp() {
        super("stophttp", 1);
    }
}
