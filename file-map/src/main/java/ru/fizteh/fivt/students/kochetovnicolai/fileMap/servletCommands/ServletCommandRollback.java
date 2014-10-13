package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Lazy
public class ServletCommandRollback extends ServletCommand {

    @Autowired
    public ServletCommandRollback(TableManager manager) {
        super("/rollback", manager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        DistributedTable table = getTable(req, resp);
        if (table == null) {
            return;
        }


        int diff;
        try {
            table.useTransaction(sessionID);
            diff = table.rollback();
            table.removeTransaction(sessionID);
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } finally {
            manager.deleteTableByID(sessionID);
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF8");
        resp.getWriter().println("diff=" + diff);
    }
}

