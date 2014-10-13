package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;


@Component
@Lazy
public class TableCommandRollback extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        Table table = manager.getCurrentTable();
        if (table == null) {
            manager.printMessage("no table");
            return false;
        }
        manager.printMessage(Integer.toString(table.rollback()));
        return true;
    }

    public TableCommandRollback() {
        super("rollback", 1);
    }
}
