package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTableProvider;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Lazy
public class ServletCommandGet extends ServletCommand {

    @Autowired
    public ServletCommandGet(TableManager manager) {
        super("/get", manager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String key = getValue("key", req, resp);
        if (key == null) {
            return;
        }

        DistributedTable table = getTable(req, resp);
        if (table == null) {
            return;
        }

        String value;
        try {
            table.useTransaction(sessionID);
            value = DistributedTableProvider.serializeByTypesList(table.getTypes(), table.get(key));
        } catch (IllegalArgumentException e) {
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
