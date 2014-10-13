package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class TableCommandDrop extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        if (!manager.existsTable(args[1])) {
            manager.printMessage(args[1] + " not exists");
            return false;
        }
        try {
            if (manager.removeTable(args[1])) {
                manager.printMessage("dropped");
                return true;
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public TableCommandDrop() {
        super("drop", 2);
    }
}
