package ru.fizteh.fivt.students.kochetovnicolai.table;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;

import java.util.ArrayList;

public class TestStoreable {

    protected ArrayList<Class<?>> types;
    protected ArrayList<Object> values;
    protected Storeable storeable;

    @Before
    public void createStoreable() {
        types = new ArrayList<>();
        types.add(Integer.class);
        types.add(Long.class);
        types.add(Double.class);
        types.add(Float.class);
        types.add(Byte.class);
        types.add(Boolean.class);
        types.add(String.class);

        values = new ArrayList<>();
        values.add(42);
        values.add(-42L);
        values.add(3.1415926535897);
        values.add(0.1234f);
        values.add(Byte.parseByte("12"));
        values.add(true);
        values.add("abracadabra");
    }

    @Test
    public void createAndGetShouldWork() {
        storeable = new TableRecord(types);
        for (int i = 0; i < values.size(); i++) {
            storeable.setColumnAt(i, values.get(i));
            Assert.assertEquals("values should be equals", values.get(i), storeable.getColumnAt(i));
        }
        storeable.setColumnAt(0, null);
        Assert.assertEquals("values should be null", null, storeable.getColumnAt(0));
    }

    @Test
    public void getIntAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, values.get(0));
        Assert.assertEquals("values should be equals", values.get(0), storeable.getIntAt(0));
    }

    @Test
    public void getLongAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(1, values.get(1));
        Assert.assertEquals("values should be equals", values.get(1), storeable.getLongAt(1));
    }

    @Test
    public void getDoubleAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(2, values.get(2));
        Assert.assertEquals("values should be equals", values.get(2), storeable.getDoubleAt(2));
    }

    @Test
    public void getFloatAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(3, values.get(3));
        Assert.assertEquals("values should be equals", values.get(3), storeable.getFloatAt(3));
    }

    @Test
    public void getByteAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(4, values.get(4));
        Assert.assertEquals("values should be equals", values.get(4), storeable.getByteAt(4));
    }

    @Test
    public void getBooleanAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(5, values.get(5));
        Assert.assertEquals("values should be equals", values.get(5), storeable.getBooleanAt(5));
    }

    @Test
        public void getStringAtShouldWork() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(6, values.get(6));
        Assert.assertEquals("values should be equals", values.get(6), storeable.getStringAt(6));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void subZeroColumnPutShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(-1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void subZeroColumnGetShouldFail() {
        storeable = new TableRecord(types);
        storeable.getColumnAt(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void sizeColumnPutShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(7, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void sizeColumnGetShouldFail() {
        storeable = new TableRecord(types);
        storeable.getColumnAt(7);
    }

    @Test(expected = ColumnFormatException.class)
    public void getIntShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(1, 0L);
        storeable.getIntAt(1);
    }

    @Test(expected = ColumnFormatException.class)
    public void getLongShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getLongAt(0);
    }

    @Test(expected = ColumnFormatException.class)
    public void getDoubleShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getDoubleAt(0);
    }

    @Test(expected = ColumnFormatException.class)
    public void getFloatShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getFloatAt(0);
    }

    @Test(expected = ColumnFormatException.class)
    public void getBooleanShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getBooleanAt(0);
    }

    @Test(expected = ColumnFormatException.class)
    public void getStringShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getStringAt(0);
    }

    @Test(expected = ColumnFormatException.class)
    public void getByteShouldFail() {
        storeable = new TableRecord(types);
        storeable.setColumnAt(0, 0);
        storeable.getByteAt(0);
    }
}
