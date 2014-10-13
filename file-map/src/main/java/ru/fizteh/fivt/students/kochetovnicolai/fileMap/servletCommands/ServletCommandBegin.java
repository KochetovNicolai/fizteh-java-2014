package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Lazy
public class ServletCommandBegin extends ServletCommand {

    @Autowired
    public ServletCommandBegin(TableManager manager) {
        super("/begin", manager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String tableName = getValue("table", req, resp);
        if (tableName == null) {
            return;
        }

        try {
            sessionID = manager.newSession(tableName);
        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        if (sessionID == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Table doesn't exists");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF8");

        char[] str = new char[5];
        for (int i = 4; i >= 0; i--) {
            str[i] = (char) ('0' + sessionID % 10);
            sessionID /= 10;
        }
        resp.getWriter().println("tid=" + new String(str));
    }
}
