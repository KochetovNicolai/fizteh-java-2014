package ru.fizteh.fivt.students.kochetovnicolai.table;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;

public class TestDistributedTableFactory {

    protected DistributedTableProviderFactory factory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createWorkingDirectoryAndFactory() {
        factory = new DistributedTableProviderFactory();
    }

    @After
    public void removeWorkingDirectoryAndFactory() {
        factory = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void createProviderEmptyShouldFail() throws IOException {
        factory.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createProviderOnFileShouldFail() throws IllegalArgumentException, IOException {
        String name = "file";
        File file = folder.newFile(name);
        factory.create(file.getAbsolutePath());
    }

    @Test
    public void createProvider() throws IOException {
        Assert.assertTrue("failed create provider", factory.create(folder.getRoot().getPath()) != null);
    }

    @Test
    public void doubleCloseShouldWork() throws IOException {
        factory.close();
        factory.close();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionAfterClose() throws IOException {
        factory.close();
        factory.create("");
    }
}
