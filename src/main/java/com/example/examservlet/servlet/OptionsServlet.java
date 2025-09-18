package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/options")
public class OptionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteOption(req, resp); break;
            default: listOptions(resp); break;
        }
    }

    private void listOptions(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT o.option_id, o.option_no, o.option_title, o.is_correct, " +
                             "q.question_id, q.question_description " +
                             "FROM options o JOIN questions q ON o.question_id = q.question_id " +
                             "ORDER BY o.question_id, o.option_no")) {

            out.println("<html><body><h1>Options</h1>");
            out.println("<a href='options?action=new'>Add New Option</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Question</th><th>No</th><th>Title</th><th>Correct</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("option_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getInt("question_id") + " - " + rs.getString("question_description") + "</td>");
                out.println("<td>" + rs.getInt("option_no") + "</td>");
                out.println("<td>" + rs.getString("option_title") + "</td>");
                out.println("<td>" + rs.getBoolean("is_correct") + "</td>");
                out.println("<td><a href='options?action=edit&option_id=" + id + "'>Edit</a> | "
                        + "<a href='options?action=delete&option_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String questionId, String no, String title) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Option Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='option_id' value='" + id + "'>");
            out.println("Question ID: <input type='number' name='question_id' value='" + questionId + "' required><br>");
            out.println("Option No: <input type='number' name='option_no' value='" + no + "' required><br>");
            out.println("Option Title: <input type='text' name='option_title' value='" + title + "' required><br>");
            out.println("Is Correct: <input type='checkbox' name='is_correct' value='1'><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='options'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("option_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM options WHERE option_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id),
                        String.valueOf(rs.getInt("question_id")),
                        String.valueOf(rs.getInt("option_no")),
                        rs.getString("option_title"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteOption(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("option_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM options WHERE option_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("options");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("option_id");
        int questionId = Integer.parseInt(req.getParameter("question_id"));
        int no = Integer.parseInt(req.getParameter("option_no"));
        String title = req.getParameter("option_title");
        boolean isCorrect = req.getParameter("is_correct") != null;

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE options SET question_id=?, option_no=?, option_title=?, is_correct=? WHERE option_id=?")) {
                    ps.setInt(1, questionId);
                    ps.setInt(2, no);
                    ps.setString(3, title);
                    ps.setBoolean(4, isCorrect);
                    ps.setInt(5, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO options (question_id, option_no, option_title, is_correct) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, questionId);
                    ps.setInt(2, no);
                    ps.setString(3, title);
                    ps.setBoolean(4, isCorrect);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("options");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM options WHERE option_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
