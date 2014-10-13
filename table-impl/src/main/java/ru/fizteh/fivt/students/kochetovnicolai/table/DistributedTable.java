package ru.fizteh.fivt.students.kochetovnicolai.table;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DistributedTable implements Table, AutoCloseable {

    private Path currentFile;
    private Path currentPath;
    private String tableName;
    private HashMap<String, Storeable> cache;
    private ThreadLocal<HashMap<String, Storeable>> changes;
    private ThreadLocal<HashMap<String, Storeable>> defaultChanges;
    private HashMap<Integer, HashMap<String, Storeable>> changesPool;

    private final int partsNumber = 16;
    private Path[] directoriesList = new Path[partsNumber];
    private Path[][] filesList = new Path[partsNumber][partsNumber];
    private Path signature;
    private List<Class<?>> types;
    private DistributedTableProvider provider;

    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock(true);
    private volatile boolean isClosed = false;

    public boolean closed() {
        return isClosed;
    }

    private void checkState() throws IllegalStateException {
        if (isClosed) {
            throw new IllegalStateException("table " + tableName + " already closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        cacheLock.writeLock().lock();
        try {
            if (isClosed) {
                return;
            }
            provider.forgetTable(tableName);
            isClosed = true;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public String getName() {
        checkState();
        return tableName;
    }

    private byte getFirstByte(String s) {
        return (byte) Math.abs(s.getBytes(StandardCharsets.UTF_8)[0]);
    }

    public static boolean isValidKey(String key) {
        return key != null && !key.equals("") && !key.matches(".*[\\s].*");
    }

    public boolean isValidValue(Storeable value) {
        checkState();
        try {
            TableRecord.checkStoreableTypes(value, types);
        } catch (IndexOutOfBoundsException e) {
            return false;
        } catch (ColumnFormatException e) {
            return false;
        }
        return true;
    }

    private int getCurrentFileLength(int dirNumber, int fileNumber) throws IOException {
        int fileRecordNumber = 0;
        currentFile = filesList[dirNumber][fileNumber];
        if (Files.size(currentFile) == 0) {
            throw new IOException(currentFile + ": empty file");
        }
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(currentFile.toFile()))) {
            String[] pair;
            try {
                while ((pair = readNextPair(inputStream)) != null) {
                    byte firstByte = getFirstByte(pair[0]);
                    if (firstByte % partsNumber != dirNumber || (firstByte / partsNumber) % partsNumber != fileNumber) {
                        throw new IOException("invalid key in file " + currentFile);
                    }
                    if (!isValidKey(pair[0])) {
                        throw new IOException("invalid key format in file " + currentFile);
                    }
                    try {
                        Storeable value = DistributedTableProvider.deserialiseByTypesList(types, pair[1]);
                        if (!isValidValue(value)) {
                            throw new IOException("invalid value format in file " + currentFile);
                        }
                        cache.put(pair[0], value);
                    } catch (ParseException e) {
                        throw new IOException("invalid value format in file " + currentFile, e);
                    }
                    fileRecordNumber++;
                }
            } catch (IOException e) {
                throw new IOException(currentFile + ": " + e.getMessage());
            }
        }
        return fileRecordNumber;
    }

    private int readTable() throws IOException {
        int directoriesNumber = 0;
        int tableSize = 0;
        if (Files.exists(signature)) {
            directoriesNumber++;
        }
        for (int i = 0; i < partsNumber; i++) {
            if (Files.exists(directoriesList[i])) {
                directoriesNumber++;
                int filesNumber = 0;
                for (int j = 0; j < partsNumber; j++) {
                    if (Files.exists(filesList[i][j])) {
                        filesNumber++;
                        tableSize += getCurrentFileLength(i, j);
                    }
                }
                int filesFoundNumber = directoriesList[i].toFile().list().length;
                if (filesFoundNumber == 0 || directoriesList[i].toFile().list().length != filesNumber) {
                    throw new IOException(directoriesList[i] + ": contains unknown files or directories");
                }
            }
        }
        if (directoriesNumber != currentPath.toFile().list().length) {
            throw new IOException("redundant files into table directory");
        }
        return tableSize;
    }

    private void createSignature(Path tableDirectory) throws IOException {
        signature = tableDirectory.resolve("signature.tsv");
        if (!Files.exists(signature)) {
            Files.createFile(signature);
        }
        try (PrintWriter output = new PrintWriter(new FileOutputStream(signature.toFile()))) {
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) {
                    output.write(' ');
                }
                output.write(TableRecord.SUPPORTED_CLASSES.get(types.get(i)));
            }
            output.write(System.lineSeparator());
        }
    }

    private List<Class<?>> getSignature(Path tableDirectory) throws IOException {
        signature = tableDirectory.resolve("signature.tsv");
        if (!Files.exists(signature) || !Files.isRegularFile(signature)) {
            throw new IOException(signature + ": file doesn't exists");
        }
        String string;
        try (BufferedReader input = new BufferedReader(new FileReader(signature.toFile()))) {
            string = input.readLine();
            if (input.read() != -1 || string == null) {
                throw new IOException(signature + ": invalid file format");
            }
        }
        String[] typesNames = string.trim().split("[\\s]+");
        ArrayList<Class<?>> typeList = new ArrayList<>(typesNames.length);
        for (String nextType : typesNames) {
            if (!TableRecord.SUPPORTED_TYPES.containsKey(nextType)) {
                throw new IOException(signature + ": invalid file format: unsupported type");
            }
            typeList.add(TableRecord.SUPPORTED_TYPES.get(nextType));
        }
        return typeList;
    }

    private void checkTableName(String name) {
        if (name == null || name.matches(".*[ \\s\\\\/].*")) {
            throw new IllegalArgumentException("invalid table name");
        }
    }

    private void initialiseTable(Path tableDir, String name, DistributedTableProvider provider) throws IOException {
        this.provider = provider;
        currentPath = tableDir;
        tableName = name;
        for (int i = 0; i < partsNumber; i++) {
            directoriesList[i] = currentPath.resolve(i + ".dir");
            for (int j = 0; j < partsNumber; j++) {
                filesList[i][j] = directoriesList[i].resolve(j + ".dat");
            }
        }
        cache = new HashMap<>();
        changes = new ThreadLocal<HashMap<String, Storeable>>() {
            @Override
            protected HashMap<String, Storeable> initialValue() {
                return new HashMap<>();
            }
        };
        defaultChanges = new ThreadLocal<>();
        changesPool = new HashMap<>();
        readTable();
    }

    public DistributedTable(DistributedTableProvider provider, Path tableDirectory, String name,
                            List<Class<?>> columnTypes) throws IOException {
        checkTableName(name);
        TableRecord.checkTypesList(columnTypes);
        if (tableDirectory == null) {
            throw new IllegalArgumentException("table directory shouldn't be null");
        }
        if (!Files.exists(tableDirectory)) {
            Files.createDirectory(tableDirectory);
        } else if (!Files.isDirectory(tableDirectory) || tableDirectory.toFile().list().length != 0) {
            throw new IOException(tableDirectory + ": not a directory or is not empty");
        }
        types = columnTypes;
        createSignature(tableDirectory);
        initialiseTable(tableDirectory, name, provider);
    }

    public DistributedTable(DistributedTableProvider provider, Path tableDirectory, String name) throws IOException {
        checkTableName(name);
        if (tableDirectory == null) {
            throw new IllegalArgumentException("table directory shouldn't be null");
        }
        if (!Files.exists(tableDirectory) || !Files.isDirectory(tableDirectory)) {
            throw new IOException(tableDirectory + ": invalid directory");
        }
        types = getSignature(tableDirectory);
        initialiseTable(tableDirectory, name, provider);
    }
    
    private int findDifference() {
        int diff = 0;
        for (String key : changes.get().keySet()) {
            if (changes.get().get(key) == null) {
                if (cache.containsKey(key)) {
                    diff++;
                }
            } else {
                if (!cache.containsKey(key) || !changes.get().get(key).equals(cache.get(key))) {
                    diff++;
                }
            }
        }
        return diff;
    }

    public int changesSize() {
        checkState();
        cacheLock.readLock().lock();
        try {
            return findDifference();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public int rollback() {
        checkState();
        int canceled = changesSize();
        changes.get().clear();
        return canceled;
    }

    @Override
     public Storeable get(String key) throws IllegalArgumentException {
        checkState();
        if (key == null || !isValidKey(key)) {
            throw new IllegalArgumentException("invalid key");
        }
        if (changes.get().containsKey(key)) {
            return changes.get().get(key);
        } else {
            cacheLock.readLock().lock();
            try {
                return cache.get(key);
            } finally {
                cacheLock.readLock().unlock();
            }
        }
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        checkState();
        if (key == null || !isValidKey(key)) {
            throw new IllegalArgumentException("invalid key");
        }
        if (value == null) {
            throw new IllegalArgumentException("invalid value");
        }
        try {
            TableRecord.checkStoreableTypes(value, types);
        } catch (IndexOutOfBoundsException e) {
            throw new ColumnFormatException(e);
        }
        Storeable old = get(key);
        changes.get().put(key, value);
        return old;
    }

    @Override
    public Storeable remove(String key) throws IllegalArgumentException {
        checkState();
        if (key == null || !isValidKey(key)) {
            throw new IllegalArgumentException("invalid key");
        }
        Storeable old = get(key);
        changes.get().put(key, null);
        return old;
    }


    @Override
    public int size() {
        checkState();
        cacheLock.readLock().lock();
        try {
            int size = cache.size();
            for (String key : changes.get().keySet()) {
                if (changes.get().get(key) == null) {
                    if (cache.containsKey(key)) {
                        size--;
                    }
                } else {
                    if (!cache.containsKey(key)) {
                        size++;
                    }
                }
            }
            return size;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public int commit() throws IOException {
        checkState();
        cacheLock.writeLock().lock();
        try {
            boolean[][] changedFiles = new boolean[partsNumber][partsNumber];
            boolean[][] removedFiles = new boolean[partsNumber][partsNumber];
            boolean[] changedFolders = new boolean[partsNumber];
            int difference = findDifference();
            for (String key : changes.get().keySet()) {
                byte first = getFirstByte(key);
                changedFiles[first % partsNumber][(first / partsNumber) % partsNumber] = true;
                changedFolders[first % partsNumber] = true;
                if (changes.get().get(key) == null) {
                    if (cache.containsKey(key)) {
                        cache.remove(key);
                        removedFiles[first % partsNumber][(first / partsNumber) % partsNumber] = true;
                    }
                } else {
                    cache.put(key, changes.get().get(key));
                }
            }
            changes.get().clear();
            HashMap<Byte, HashMap<String, String>> serealized = new HashMap<>();
            for (String key : cache.keySet()) {
                byte first = getFirstByte(key);
                if (!serealized.containsKey(first)) {
                    serealized.put(first, new HashMap<String, String>());
                }
                serealized.get(first).put(key, DistributedTableProvider.serializeByTypesList(types, cache.get(key)));
            }
            for (int i = 0; i < partsNumber; i++) {
                if (changedFolders[i]) {
                    if (!Files.exists(directoriesList[i])) {
                        Files.createDirectory(directoriesList[i]);
                    }
                    for (int j = 0; j < partsNumber; j++) {
                        if (changedFiles[i][j]) {
                            if (Files.exists(filesList[i][j]) && removedFiles[i][j]) {
                                Files.delete(filesList[i][j]);
                            }
                            byte first = (byte) (i + partsNumber * j);
                            if (serealized.containsKey(first)) {
                                if (!Files.exists(filesList[i][j])) {
                                    Files.createFile(filesList[i][j]);
                                }
                                HashMap<String, String> map = serealized.get(first);
                                try (DataOutputStream outputStream = new
                                        DataOutputStream(new FileOutputStream(filesList[i][j].toFile(), true))) {
                                    for (String key : map.keySet()) {
                                        writeNextPair(outputStream, key, map.get(key));
                                    }
                                }
                            }
                        }
                    }
                    if (directoriesList[i].toFile().list().length == 0) {
                        Files.delete(directoriesList[i]);
                    }
                }
            }
            return difference;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public int getColumnsCount() {
        checkState();
        return types.size();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        checkState();
        return types.get(columnIndex);
    }

    public List<Class<?>> getTypes() {
        checkState();
        return types;
    }

    private void writeNextPair(DataOutputStream outputStream, String key, String value) throws IOException {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        outputStream.writeInt(keyBytes.length);
        outputStream.writeInt(valueBytes.length);
        outputStream.write(keyBytes);
        outputStream.write(valueBytes);
    }

    private String[] readNextPair(DataInputStream inputStream) throws IOException {
        if (inputStream.available() == 0) {
            return null;
        }
        int keySize;
        int valueSize;
        try {
            keySize = inputStream.readInt();
            valueSize = inputStream.readInt();
        } catch (IOException e) {
            throw new EOFException("the file is corrupt or has an incorrect format");
        }
        if (keySize < 1 || valueSize < 1 || inputStream.available() < keySize
                || inputStream.available() < valueSize || inputStream.available() < keySize + valueSize) {
            throw new EOFException("the file is corrupt or has an incorrect format");
        }
        byte[] keyBytes = new byte[keySize];
        byte[] valueBytes = new byte[valueSize];
        if (inputStream.read(keyBytes) != keySize || inputStream.read(valueBytes) != valueSize) {
            throw new EOFException("the file is corrupt or has an incorrect format");
        }
        String[] pair = new String[2];
        pair[0] = new String(keyBytes, StandardCharsets.UTF_8);
        pair[1] = new String(valueBytes, StandardCharsets.UTF_8);
        return pair;
    }

    /*
    private String readValue(String key) throws IOException {
        if (currentFile == null || !Files.exists(currentFile)) {
            return null;
        }
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(currentFile.toFile()))) {
            String[] pair;
            while ((pair = readNextPair(inputStream)) != null) {
                if (pair[0].equals(key)) {
                    inputStream.close();
                    return pair[1];
                }
            }
        }
        return null;
    }
    */

    public void clear() throws IOException {
        checkState();
        cacheLock.writeLock().lock();
        try {
            for (int i = 0; i < partsNumber; i++) {
                for (int j = 0; j < partsNumber; j++) {
                    if (Files.exists(filesList[i][j]) && !Files.exists(filesList[i][j])) {
                        throw new IOException(filesList[i][j] + ": couldn't remove file");
                    }
                }
                if (Files.exists(directoriesList[i]) && !Files.exists(directoriesList[i])) {
                    throw new IOException(directoriesList[i] + ": couldn't remove directory");
                }
            }
            Files.delete(signature);
            Files.delete(currentPath);
            close();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        checkState();
        return this.getClass().getSimpleName() + '[' + currentPath.toAbsolutePath() + ']';
    }

    public void useTransaction(Integer transactionID) {
        checkState();
        if (defaultChanges.get() == null) {
            defaultChanges.set(changes.get());
        }
        if (!changesPool.containsKey(transactionID)) {
            changesPool.put(transactionID, new HashMap<String, Storeable>());
        }
        changes.set(changesPool.get(transactionID));
    }

    public void removeTransaction(Integer transactionID) {
        checkState();
        HashMap<String, Storeable> transaction = changesPool.get(transactionID);
        if (transaction != null) {
            if (transaction == changes.get()) {
                changes.set(defaultChanges.get());
            }
            changesPool.remove(transactionID);
        }
    }

    public void setDefaultTransaction() {
        //checkState();
        HashMap<String, Storeable> transaction = defaultChanges.get();
        if (transaction != null) {
            changes.set(transaction);
        }
    }
}
