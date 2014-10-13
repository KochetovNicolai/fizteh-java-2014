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
public class TableCommandGet extends Executable {

    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        Table table = manager.getCurrentTable();
        if (table == null) {
            manager.printMessage("no table");
            return false;
        }
        Storeable value = table.get(args[1]);
        if (value == null) {
            manager.printMessage("not found");
        } else {
            manager.printMessage("found");
            String stringValue;
            try {
                stringValue = manager.serialize(table.get(args[1]));
            } catch (ParseException e) {
                manager.printMessage("wrong type (" + e.getMessage() + ")");
                return false;
            }
            manager.printMessage(stringValue);
        }
        return true;
    }

    public TableCommandGet() {
        super("get", 2);
    }
}
