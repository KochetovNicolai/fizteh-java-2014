package ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Launcher {

    private HashMap<String, Executable> commands;
    private StringParser stringParser;

    public Launcher(HashMap<String, Executable> commandsMap, StringParser parser) {
        commands = commandsMap;
        stringParser = parser;
    }

    public boolean launch(String[] args, Manager manager) throws IOException {
        if (args.length == 0) {
            return exec(System.in, manager, false);
        } else {

            StringBuilder builder = new StringBuilder();

            for (String arg : args) {
                builder.append(arg);
                builder.append(" ");
            }
            String string = builder.toString().replaceAll(";", "\n");

            byte[] bytes = string.getBytes("UTF-8");
            InputStream inputStream = new ByteArrayInputStream(bytes);

            return exec(inputStream, manager, true);
        }
    }

    private boolean exec(InputStream input, Manager manager, boolean isPackage) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        while (true) {
            if (!isPackage) {
                manager.printSuggestMessage();
            }
            String commandSet = reader.readLine();
            if (commandSet == null) {
                break;
            }
            String[] commandList = commandSet.split(";");
            for (String commandName : commandList) {
                String[] tokens = stringParser.parse(commandName);

                if (tokens.length == 0 || tokens[0].equals("")) {
                    continue;
                }

                boolean success = false;

                if (!commands.containsKey(tokens[0])) {
                    manager.printMessage(tokens[0] + ": command not found");
                } else {
                    Executable command = commands.get(tokens[0]);
                    int argumentsNumber = command.getArgumentsNumber();
                    if ((argumentsNumber > 0 && argumentsNumber != tokens.length)
                            || (argumentsNumber <= 0 && tokens.length < -argumentsNumber)) {
                        manager.printMessage(tokens[0] + ": invalid number of arguments");
                    } else {
                        success = command.execute(tokens);
                    }
                }

                if (manager.timeToExit()) {
                    return true;
                }

                if (!success && isPackage) {
                    manager.setExit();
                    return false;
                }
            }
        }
        manager.setExit();
        return true;
    }
}
