package ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore;

import java.io.PrintStream;

public abstract class Manager {

    protected boolean mustExit = false;

    protected PrintStream outputStream = System.out;

    public boolean timeToExit() {
        return mustExit;
    }

    public void printMessage(final String message) {
        outputStream.println(message);
    }

    public void printSuggestMessage() {
        outputStream.print("$ ");
    }

    public void setExit() {
        mustExit = true;
    }
}
