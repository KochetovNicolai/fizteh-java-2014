package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class TableCommandUse extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        if (!manager.existsTable(args[1])) {
            manager.printMessage(args[1] + " not exists");
            return false;
        }
        try {
            DistributedTable table = manager.getCurrentTable();
            if (table != null && table.changesSize() != 0) {
                manager.printMessage(Integer.toString(table.changesSize()) + " unsaved changes");
                return false;
            }
            manager.setCurrentTable(manager.getTable(args[1]));
            manager.printMessage("using " + args[1]);
            return true;
        } catch (IllegalArgumentException e) {
            manager.printMessage(e.getMessage());
            return false;
        }
    }

    public TableCommandUse() {
        super("use", 2);
    }
}
