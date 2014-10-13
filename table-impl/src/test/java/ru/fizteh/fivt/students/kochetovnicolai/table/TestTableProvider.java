package ru.fizteh.fivt.students.kochetovnicolai.table;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

@RunWith(Theories.class)
public class TestTableProvider {
    protected DistributedTableProviderFactory factory;
    protected DistributedTableProvider provider;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createWorkingDirectoryAndProvider() throws IOException {
        factory = new DistributedTableProviderFactory();
        provider = factory.create(folder.getRoot().getPath());
    }

    @After
    public void removeWorkingDirectoryAndProvider() {
        provider = null;
        factory = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeTableEmptyShouldFail() throws IOException {
       provider.removeTable(null);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @DataPoints
    public static String[] argumentsWithBadSymbols = new String [] {
            "",
            ".",
            "..",
            "....",
            "...dir",
            "\\",
            "dir/17.dir",
    };

    @Theory
    public void removeTableBadSymbolShouldFail(String name) throws IOException {
        thrown.expect(IllegalArgumentException.class);
        provider.removeTable(name);
    }

    @Test
    public void removeNotExistingTableShouldFail() throws IOException {
        thrown.expect(IllegalStateException.class);
        provider.removeTable("test");
    }

    @Theory
    public void createTableBadSymbolShouldFail(String name) throws IOException {
        thrown.expect(IllegalArgumentException.class);
        ArrayList<Class<?>> type = new ArrayList<>();
        type.add(String.class);
        provider.createTable(name, type);
    }

    @Theory
    public void getTableBadSymbolShouldFail(String name) {
        thrown.expect(IllegalArgumentException.class);
        provider.getTable(name);
    }

    @Test
    public void getTableShouldGetNullIfTableDoesNotExists() {
        Assert.assertEquals("getTable should return null", provider.getTable("abcd"), null);
    }

    @Test
    public void createTableShouldBeOK() throws IOException {
        ArrayList<Class<?>> type = new ArrayList<>();
        type.add(String.class);
        Table table = provider.createTable("abcd", type);
        Assert.assertTrue("table shouldn't be null", table != null);
        Table table2 = provider.createTable("abcd", type);
        /***/
        Assert.assertEquals("createTable should return null on the same names", null, table2);
        //
        table2 = provider.getTable("abcd");
        /***/
        Assert.assertEquals("getTable should return same objects on the same names", table, table2);
        //
        provider.removeTable("abcd");
        Assert.assertEquals("getTable should return null after remove", provider.getTable("abcd"), null);
        table = provider.createTable("abcd", type);
        Assert.assertTrue("createTable should return table after remove", table != null);
    }

    @Test
    public void serializeAndDeserializeShouldWork() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        types.add(Long.class);
        types.add(Double.class);
        types.add(Float.class);
        types.add(Byte.class);
        types.add(Boolean.class);
        types.add(String.class);

        ArrayList<Object> values = new ArrayList<>();
        values.add(42);
        values.add(null);
        values.add(3.1415926535897);
        values.add(0.1234f);
        values.add(Byte.parseByte("12"));
        values.add(true);
        values.add("abracadabra");

        Table table = provider.createTable("AllStars", types);
        StringBuilder builder = new StringBuilder("<row>");
        Storeable storeable = provider.createFor(table);
        for (int i = 0; i < values.size(); i++) {
            storeable.setColumnAt(i, values.get(i));
            if (values.get(i) != null) {
                builder.append("<col>");
                builder.append(values.get(i));
                builder.append("</col>");
            } else {
                builder.append("<null/>");
            }
        }
        builder.append("</row>");
        String pattern = builder.toString();
        String value = provider.serialize(table, storeable);
        Assert.assertEquals("serialize should return '" + pattern + "' but it return '" + value + "'", value, pattern);
        Assert.assertEquals("deserialize should return equals object", storeable, provider.deserialize(table, pattern));
    }

    @Test(expected = ParseException.class)
    public void deserializeShouldFailWithLackOfArguments() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        types.add(String.class);
        Table table = provider.createTable("table", types);
        provider.deserialize(table, "<row><col>abcd></col></row>");
    }

    @Test(expected = ParseException.class)
    public void deserializeShouldFailWithIncorrectValues() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        types.add(String.class);
        Table table = provider.createTable("table", types);
        provider.deserialize(table, "<row><col>3.14</col><col>abcd></col></row>");
    }

    @Test(expected = ParseException.class)
    public void deserializeShouldFailWithExcessOfArguments() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        types.add(String.class);
        Table table = provider.createTable("table", types);
        provider.deserialize(table, "<row><col>42</col><col>abcd></col><null/></row>");
    }

    @Test
    public void parallelPut() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        Table table = provider.createTable("table", types);

        Storeable storeable = provider.createFor(table);
        storeable.setColumnAt(0, 0);
        table.put("key0", storeable);
        table.commit();

        Thread thread1 = new Thread() {
            public void run() {
                Table table = provider.getTable("table");
                for (int i = 0; i < 100; i++) {
                    Storeable storeable = provider.createFor(table);
                    storeable.setColumnAt(0, i);
                    table.put("key" + i, storeable);
                    if (i % 5 == 0) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                }
                try {
                    Assert.assertEquals("should be commeted 99 values", 99, table.commit());
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        };

        Thread thread2 = new Thread() {
            public void run() {
                Table table = provider.getTable("table");
                Storeable storeable = provider.createFor(table);
                for (int i = 0; i < 100; i++) {
                    storeable.setColumnAt(0, i);
                    table.put("key" + i + 100, storeable);
                    if (i % 5 == 0) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                }
                try {
                    Assert.assertEquals("should be commeted 100 values", 100, table.commit());
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        };

        thread1.run();
        thread2.run();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals("200 values could be added", 200, table.size());
    }

    @Test
    public void testConcurrentPutAndCommit() throws IOException, ParseException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        Table table = provider.createTable("table", types);

        Storeable storeable = provider.createFor(table);
        storeable.setColumnAt(0, 0);
        table.put("key0", storeable);
        table.commit();

        Thread thread1 = new Thread() {
            public void run() {
                Table table = provider.getTable("table");

                Storeable storeable = provider.createFor(table);
                for (int i = 0; i < 1; i++) {
                    storeable.setColumnAt(0, i);
                    table.put("key" + i, storeable);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail(e.getMessage());
                }
                try {
                    Assert.assertEquals("should be commeted 1 values", 1, table.commit());
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
                //System.out.println("OK");
            }
        };

        Thread thread2 = new Thread() {
            public void run() {
                Table table = provider.getTable("table");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Assert.fail(e.getMessage());
                }
                Storeable storeable = provider.createFor(table);
                for (int i = 0; i < 1; i++) {
                    storeable.setColumnAt(0, i + 1);
                    table.put("key" + i, storeable);
                }
                try {
                    Assert.assertEquals("should be commeted 1 values", 1, table.commit());
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
                //System.out.println("OK2");
            }
        };

        thread2.run();
        thread1.run();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void doubleCloseShouldWork() throws IOException {
        provider.close();
        provider.close();

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionAfterClose() throws IOException {
        provider.close();
        provider.toString();
    }

    @Test
    public void factoryShouldReturnNewProviderAfterClose() throws IOException {
        provider.close();
        Assert.assertNotEquals("closed provider, but factory returned same", provider,
                factory.create(folder.getRoot().getPath()));
    }
}
