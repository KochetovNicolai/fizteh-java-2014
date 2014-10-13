package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

import java.text.ParseException;

@Component
@Lazy
public class TableCommandPut extends Executable {

    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        Table table = manager.getCurrentTable();
        if (table == null) {
            manager.printMessage("no table");
            return false;
        }
        Storeable storeable;
        try {
            storeable = manager.deserialize(args[2]);
        } catch (ParseException e) {
            manager.printMessage("wrong type (" + e.getMessage() + ")");
            return false;
        }
        Storeable oldValue = table.put(args[1], storeable);
        String oldString;
        try {
            oldString = manager.serialize(oldValue);
        } catch (ParseException e) {
            manager.printMessage("wrong type (" + e.getMessage() + ")");
            return false;
        }
        if (oldValue == null) {
            manager.printMessage("new");
        } else {
            manager.printMessage("overwrite");
            manager.printMessage(oldString);
        }
        return true;
    }

    public TableCommandPut() {
        super("put", 3);
    }
}
