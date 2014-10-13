package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProvider;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;


@Component
@Lazy
public class ServletCommandPut extends ServletCommand {

    @Autowired
    public ServletCommandPut(TableManager manager) {
        super("/put", manager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String key = getValue("key", req, resp);
        if (key == null) {
            return;
        }
        String value = getValue("value", req, resp);
        if (value == null) {
            return;
        }

        DistributedTable table = getTable(req, resp);
        if (table == null) {
            return;
        }

        try {
            table.useTransaction(sessionID);
            Storeable storeable = DistributedTableProvider.deserialiseByTypesList(table.getTypes(), value);
            value = DistributedTableProvider.serializeByTypesList(table.getTypes(), table.put(key, storeable));
        } catch (IllegalArgumentException | ParseException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            if (table.closed()) {
                manager.deleteTableByID(sessionID);
            }
            return;
        } finally {
            table.setDefaultTransaction();
        }

        if (value == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, key + ": no such key");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF8");
        resp.getWriter().println(value);
    }
}
