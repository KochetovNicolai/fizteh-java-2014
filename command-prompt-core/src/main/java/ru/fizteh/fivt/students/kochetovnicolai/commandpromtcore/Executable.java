package ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore;

public abstract class Executable {

    protected String name;
    protected int argumentsNumber;

    public Executable(String name, int argumentsNumber) {
        this.name = name;
        this.argumentsNumber = argumentsNumber;
    }

    public abstract boolean execute(String[] args);

    public String getName() {
        return name;
    }

    public int getArgumentsNumber() {
        return argumentsNumber;
    }

}
