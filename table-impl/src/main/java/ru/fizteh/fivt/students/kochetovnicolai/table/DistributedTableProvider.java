package ru.fizteh.fivt.students.kochetovnicolai.table;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.stream.*;

public class DistributedTableProvider implements TableProvider, AutoCloseable {

    private HashMap<String, DistributedTable> tables;
    private Path currentPath;
    private ReadWriteLock tablesLock;
    private volatile boolean isClosed = false;
    private DistributedTableProviderFactory factory;

    private void checkState() throws IllegalArgumentException {
        if (isClosed) {
            throw new IllegalStateException("table provider already closed");
        }
    }

    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        tablesLock.writeLock().lock();
        try {
            if (isClosed) {
                return;
            }
            factory.forgetTableProvider(currentPath);
            for (DistributedTable table : tables.values()) {
                table.close();
            }
            isClosed = true;
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    public static boolean isValidName(String name) {
        return name != null && !name.equals("") && !name.contains(".") && !name.contains("/") && !name.contains("\\");
    }

    private boolean loadTable(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid table name");
        }
        if (!tables.containsKey(name)) {
            try {
                tables.put(name, new DistributedTable(this, currentPath.resolve(name), name));
            } catch (IOException e) {
                return false;
            }
        }
        return tables.containsKey(name);
    }

    public boolean existsTable(String name) {
        checkState();
        tablesLock.readLock().lock();
        try {
            if (tables.containsKey(name)) {
                return true;
            }
        } finally {
            tablesLock.readLock().unlock();
        }
        tablesLock.writeLock().lock();
        try {
            return loadTable(name);
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    public DistributedTableProvider(Path workingDirectory, DistributedTableProviderFactory factory) throws IOException {
        this.factory = factory;
        currentPath = workingDirectory;
        if (currentPath == null) {
            throw new IllegalArgumentException("working directory shouldn't be null");
        }
        if (Files.exists(currentPath) && !Files.isDirectory(currentPath)) {
            throw new IllegalArgumentException("couldn't create working directory on file");
        }
        if (!Files.exists(currentPath)) {
            Files.createDirectories(currentPath);
        }
        tables = new HashMap<>();
        tablesLock = new ReentrantReadWriteLock(true);
    }

    @Override
    public DistributedTable getTable(String name) throws IllegalArgumentException {
        checkState();
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid table name");
        }
        tablesLock.readLock().lock();
        try {
            if (tables.containsKey(name)) {
                return tables.get(name);
            }
        } finally {
            tablesLock.readLock().unlock();
        }
        tablesLock.writeLock().lock();
        try {
            loadTable(name);
            return tables.get(name);
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    @Override
    public DistributedTable createTable(String name, List<Class<?>> columnTypes) throws IOException {
        checkState();
        if (columnTypes == null || columnTypes.size() == 0) {
            throw new IllegalArgumentException("invalid column type");
        }
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid table name");
        }
        tablesLock.writeLock().lock();
        try {
            if (loadTable(name)) {
                return null;
            }
            tables.put(name, new DistributedTable(this, currentPath.resolve(name), name, columnTypes));
            return tables.get(name);
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    public void forgetTable(String name) {
        checkState();
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid table name");
        }
        tablesLock.writeLock().lock();
        try {
            tables.remove(name);
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    @Override
    public void removeTable(String name) throws IOException {
        checkState();
        if (!isValidName(name)) {
            throw new IllegalArgumentException("invalid table name");
        }

        tablesLock.writeLock().lock();
        try {
            if (!loadTable(name)) {
                throw new IllegalStateException("table is not exists");
            }
            tables.get(name).clear();
            tables.remove(name);
        } finally {
            tablesLock.writeLock().unlock();
        }
    }

    @Override
    public TableRecord createFor(Table table) {
        checkState();
        if (table == null) {
            throw new IllegalArgumentException("argument shouldn't be null");
        }
        tablesLock.readLock().lock();
        try {
            if (!tables.containsKey(table.getName())) {
                throw new IllegalArgumentException("invalid table");
            }
            return new TableRecord(tables.get(table.getName()).getTypes());
        } finally {
            tablesLock.readLock().unlock();
        }
    }

    @Override
    public TableRecord createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        checkState();
        if (values == null) {
            throw new IllegalArgumentException("list of values shouldn't be null");
        }
        TableRecord record = createFor(table);
        if (values.size() != record.size()) {
            throw new IndexOutOfBoundsException("expected list with size " + record.size() + ", but with "
                    + values.size() + " size was received");
        }
        for (int i = 0; i < values.size(); i++) {
            record.setColumnAt(i, values.get(i));
        }
        return record;
    }

    public static TableRecord deserialiseByTypesList(List<Class<?>> types, String value) throws ParseException {
        if (value == null) {
            return null;
        }
        TableRecord record = new TableRecord(types);

        try {
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(value));
            if (!streamReader.hasNext() || streamReader.next() != XMLStreamConstants.START_ELEMENT) {
                throw new ParseException(value, 0);
            }
            if (!streamReader.getName().getLocalPart().equals("row")) {
                throw new ParseException(value, 0);
            }
            for (int i = 0; i < record.size(); i++) {
                if (!streamReader.hasNext()) {
                    throw new ParseException(value, 0);
                }
                int next = streamReader.next();
                if (next == XMLStreamConstants.START_ELEMENT) {
                    if (streamReader.getName().getLocalPart().equals("null")) {
                        record.setColumnAt(i, null);
                        if (!streamReader.hasNext() || streamReader.next() != XMLStreamConstants.END_ELEMENT
                                || !streamReader.getName().getLocalPart().equals("null")) {
                            throw new ParseException(value, 0);
                        }
                    } else if (streamReader.getName().getLocalPart().equals("col")) {
                        String text = "";
                        if (!streamReader.hasNext()) {
                            throw new ParseException(value, 0);
                        }
                        next = streamReader.next();
                        if (streamReader.hasText()) {
                            if (next != XMLStreamConstants.CHARACTERS) {
                                throw new ParseException(value, 0);
                            }
                            text = streamReader.getText();
                            if (!streamReader.hasNext()) {
                                throw new ParseException(value, 0);
                            }
                            next = streamReader.next();
                        }
                        try {
                            record.setColumnFromStringAt(i, text);
                        } catch (IllegalArgumentException e) {
                            throw new ParseException(value, 0);
                        }
                        if (next != XMLStreamConstants.END_ELEMENT
                                || !streamReader.getName().getLocalPart().equals("col")) {
                            throw new ParseException(value, 0);
                        }
                    } else {
                        throw new ParseException(value, 0);
                    }
                } else {
                    throw new ParseException(value, 0);
                }
            }
            if (!streamReader.hasNext() || streamReader.next() != XMLStreamConstants.END_ELEMENT) {
                throw new ParseException(value, 0);
            }
            if (!streamReader.getName().getLocalPart().equals("row")) {
                throw new ParseException(value, 0);
            }
        } catch (XMLStreamException e) {
            throw new ParseException(e.getMessage(), 0);
        }
        return record;
    }

    @Override
    public TableRecord deserialize(Table table, String value) throws ParseException {
        checkState();
        if (value == null) {
            return null;
        }
        TableRecord record = createFor(table);
        return deserialiseByTypesList(record.getTypes(), value);
    }

    public static String serializeByTypesList(List<Class<?>> tableTypes, Storeable storeable) {
        if (storeable == null) {
            return null;
        }
        TableRecord.checkStoreableTypes(storeable, tableTypes);
        StringWriter stringWriter = new StringWriter();
        try {
            XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
            streamWriter.writeStartElement("row");
            for (int i = 0; i < tableTypes.size(); i++) {
                Object next;
                try {
                    next = storeable.getColumnAt(i);
                } catch (IndexOutOfBoundsException e) {
                    throw new ColumnFormatException("lack of values at storeable", e);
                }
                if (next == null) {
                    streamWriter.writeEmptyElement("null");
                } else {
                    streamWriter.writeStartElement("col");
                    String string = next.toString();
                    streamWriter.writeCharacters(string);
                    streamWriter.writeEndElement();
                }
            }
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("error while converting into xml");
        }
        return stringWriter.toString();
    }

    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        checkState();
        if (table == null) {
            throw new IllegalArgumentException("table shouldn't be null");
        }
        List<Class<?>> tableTypes;
        tablesLock.readLock().lock();
        try {
            if (!tables.containsKey(table.getName())) {
                throw new IllegalArgumentException("invalid arguments");
            }
            if (value == null) {
                return null;
            }
            tableTypes = tables.get(table.getName()).getTypes();
        } finally {
            tablesLock.readLock().unlock();
        }
        return serializeByTypesList(tableTypes, value);
    }

    @Override
    public String toString() {
        checkState();
        return this.getClass().getSimpleName() + '[' + currentPath.toAbsolutePath() + ']';
    }
}
