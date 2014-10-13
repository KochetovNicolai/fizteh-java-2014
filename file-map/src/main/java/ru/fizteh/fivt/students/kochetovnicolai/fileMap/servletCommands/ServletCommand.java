package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.fivt.students.kochetovnicolai.table.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class ServletCommand extends HttpServlet {

    protected TableManager manager;
    protected String name;
    Integer sessionID = null;

    public ServletCommand(String name, TableManager manager) {//TableManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("manager shouldn't be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name shouldn't be null");
        }
        this.name = name;
        //this.manager = manager;
    }

    public String getName() {
        return name;
    }

    protected DistributedTable getTable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = getValue("tid", req, resp);
        if (id == null) {
            return null;
        }
        if (!id.matches("[0-9]{5}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, id + ": invalid tid format");
            return null;
        }
        sessionID = Integer.parseInt(id);
        DistributedTable table = manager.getTableByID(sessionID);
        if (table == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, id + ": unused tid");
            return null;
        }
        return table;
    }

    protected String getValue(String name, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String value = req.getParameter(name);
        if (value == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, name + " expected");
            return null;
        }
        return value;
    }
}
