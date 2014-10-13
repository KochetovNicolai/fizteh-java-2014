package ru.fizteh.fivt.students.kochetovnicolai.fileMap.tableCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Executable;

@Component
@Lazy
public class TableCommandStartHttp extends Executable {
    @Autowired
    TableManager manager;

    @Override
    public boolean execute(String[] args) {
        if (args.length > 2) {
            manager.printMessage(getName() + ": invalid number of arguments");
            return false;
        }
        int port = 10001;
        if (args.length == 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (ClassCastException e) {
                manager.printMessage("not started: " + args[1] + ": wrong port number");
                return false;
            }
        }
        return manager.startHTTP(port);
    }

    public TableCommandStartHttp() {
        super("starthttp", -1);
    }
}
