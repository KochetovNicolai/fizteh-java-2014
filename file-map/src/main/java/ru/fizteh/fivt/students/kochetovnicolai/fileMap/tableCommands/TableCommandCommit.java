package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

import java.io.IOException;

@Component
@Lazy
public class TableCommandCommit extends Executable {

    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        Table table = manager.getCurrentTable();
        if (table == null) {
            manager.printMessage("no table");
            return false;
        }
        try {
            manager.printMessage(Integer.toString(table.commit()));
        } catch (IOException e) {
            manager.printMessage(e.getMessage());
            return false;
        }
        return true;
    }

    public TableCommandCommit() {
        super("commit", 1);
    }
}
