package ru.fizteh.fivt.students.kochetovnicolai.shell;

import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.commandpromtcore.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
public class FileManager extends Manager {

    protected File currentPath = new File("").getAbsoluteFile();

    @Override
    public void printSuggestMessage() {
        outputStream.print(currentPath.getName() + File.separator + "$ ");
    }

    public File getCurrentPath() {
        return currentPath;
    }

    public boolean setCurrentPath(File newCurrentPath) {
        try {
            currentPath = newCurrentPath.getCanonicalFile();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public File resolvePath(String path) {
        File newPath = new File(path);
        if (newPath.exists() && newPath.isAbsolute()) {
            try {
                return newPath.getCanonicalFile();
            } catch (IOException e) {
                return null;
            }
        }
        newPath = new File(currentPath.getAbsolutePath());
        String[] directories = path.split("\\" + File.separator);
        for (String directory : directories) {
            if (!directory.equals(".")) {
                if (directory.equals("..")) {
                    if (newPath.getParent() != null) {
                        newPath = newPath.getParentFile();
                    }
                } else {
                    newPath = new File(newPath.getAbsolutePath() + File.separator + directory);
                }
            }
        }
        return newPath;
    }

    boolean copy(File source, File destination, String command) {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            while (true) {
                int length = inputStream.read(buffer);
                if (length < 0) {
                    break;
                }
                outputStream.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            printMessage(command + ": /'" + destination.getAbsolutePath() + "\': file already exists");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    printMessage(command + ": /'" + source.getAbsolutePath() + "\': cannot close file");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    printMessage(command + ": /'" + destination.getAbsolutePath() + "\': cannot close file");
                }
            }
        }
        return false;
    }

    boolean recursiveCopy(File source, File destination, String command) {
        if (source.isDirectory()) {
            try {
                if (!destination.mkdir()) {
                    printMessage(command + ": \'" + destination.getAbsolutePath() + "\': couldn't create directory");
                    return false;
                }
            } catch (SecurityException e) {
                printMessage(command + ": \'" + destination.getAbsolutePath() + "\': no rights to create directory");
                return false;
            }
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    File newFile = new File(destination.getAbsolutePath() + File.separator + file.getName());
                    if (!recursiveCopy(file, newFile, command)) {
                        return false;
                    }
                }
            }
        } else {
            try {
                if (!destination.createNewFile()) {
                    printMessage(command + ": \'" + destination.getAbsolutePath() + "\': couldn't create file");
                    return false;
                }
                if (!destination.canWrite() || !source.canRead()) {
                    printMessage(command + ": \'" + destination.getAbsolutePath() + "\': no rights to create file");
                    return false;
                }
                return copy(source, destination, command);

            } catch (SecurityException e) {
                printMessage(command + ": \'" + destination.getAbsolutePath() + "\': haven't rights to create file");
                return false;
            } catch (IOException e) {
                printMessage(command + ": \'" + destination.getAbsolutePath() + "\': couldn't create file");
                return false;
            }
        }
        return true;
    }

    protected boolean safeCopy(File source, File destination, String command) {
        if (source == null) {
            printMessage(command + ": invalid source path");
        } else if (!source.exists()) {
            printMessage(command + ": source path doesn't exists");
        } else if (destination == null) {
            printMessage(command + ": invalid destination path");
        } else if (!destination.getParentFile().exists()) {
            printMessage(command + ": destination path doesn't exists");
        } else if (source.isDirectory() && destination.isFile()) {
            printMessage(command + ": source path is a directory, but destination is not");
        } else {
            return recursiveCopy(source, destination, command);
        }
        return false;
    }

    protected boolean recursiveRemove(File removable, String command) {
        if (removable.isDirectory()) {
            File[] files = removable.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!recursiveRemove(file, command)) {
                        return false;
                    }
                }
            }
        }
        try {
            if (!removable.delete()) {
                printMessage(command + ": couldn't remove file \'" + removable.getAbsolutePath() + "\'");
                return false;
            }
            return true;
        } catch (SecurityException e) {
            printMessage(command + ": couldn't remove file \'" + removable.getAbsolutePath() + "\'");
        }
        return false;
    }
}
