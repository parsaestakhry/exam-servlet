package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/exams")
public class ExamsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteExam(req, resp); break;
            default: listExams(resp); break;
        }
    }

    private void listExams(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT e.exam_id, e.exam_title, e.exam_date, e.percentile, " +
                             "c.course_id, c.course_title " +
                             "FROM exams e JOIN courses c ON e.course_id = c.course_id " +
                             "ORDER BY e.exam_id")) {

            out.println("<html><body><h1>Exams</h1>");
            out.println("<a href='exams?action=new'>Add New Exam</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Title</th><th>Date</th><th>Percentile</th><th>Course</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("exam_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getString("exam_title") + "</td>");
                out.println("<td>" + rs.getDate("exam_date") + "</td>");
                out.println("<td>" + rs.getBigDecimal("percentile") + "</td>");
                out.println("<td>" + rs.getInt("course_id") + " - " + rs.getString("course_title") + "</td>");
                out.println("<td><a href='exams?action=edit&exam_id=" + id + "'>Edit</a> | "
                        + "<a href='exams?action=delete&exam_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String title, String date, String percentile) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Exam Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='exam_id' value='" + id + "'>");
            out.println("Course ID: <input type='number' name='course_id' required><br>");
            out.println("Title: <input type='text' name='exam_title' value='" + title + "' required><br>");
            out.println("Date: <input type='date' name='exam_date' value='" + date + "'><br>");
            out.println("Percentile: <input type='number' step='0.01' name='percentile' value='" + percentile + "'><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='exams'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("exam_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM exams WHERE exam_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id),
                        rs.getString("exam_title"),
                        rs.getDate("exam_date") != null ? rs.getDate("exam_date").toString() : "",
                        rs.getBigDecimal("percentile") != null ? rs.getBigDecimal("percentile").toString() : "");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteExam(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("exam_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM exams WHERE exam_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("exams");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("exam_id");
        int courseId = Integer.parseInt(req.getParameter("course_id"));
        String title = req.getParameter("exam_title");
        String date = req.getParameter("exam_date");
        String percentile = req.getParameter("percentile");

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE exams SET course_id=?, exam_title=?, exam_date=?, percentile=? WHERE exam_id=?")) {
                    ps.setInt(1, courseId);
                    ps.setString(2, title);
                    ps.setDate(3, date == null || date.isEmpty() ? null : Date.valueOf(date));
                    ps.setBigDecimal(4, percentile == null || percentile.isEmpty() ? null : new java.math.BigDecimal(percentile));
                    ps.setInt(5, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO exams (course_id, exam_title, exam_date, percentile) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, courseId);
                    ps.setString(2, title);
                    ps.setDate(3, date == null || date.isEmpty() ? null : Date.valueOf(date));
                    ps.setBigDecimal(4, percentile == null || percentile.isEmpty() ? null : new java.math.BigDecimal(percentile));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("exams");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM exams WHERE exam_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
