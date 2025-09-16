package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/courses")
public class CoursesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteCourse(req, resp); break;
            default: listCourses(resp); break;
        }
    }

    private void listCourses(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM courses ORDER BY course_id")) {

            out.println("<html><body><h1>Courses</h1>");
            out.println("<a href='courses?action=new'>Add New Course</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Title</th><th>Unit</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("course_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getString("course_title") + "</td>");
                out.println("<td>" + rs.getInt("unit_no") + "</td>");
                out.println("<td><a href='courses?action=edit&course_id=" + id + "'>Edit</a> | "
                        + "<a href='courses?action=delete&course_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }

            out.println("</table><br><a href='/'>Back</a></body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String title, String unit) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Course Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='course_id' value='" + id + "'>");
            out.println("Title: <input type='text' name='course_title' value='" + title + "' required><br>");
            out.println("Unit No: <input type='number' name='unit_no' value='" + unit + "'><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='courses'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("course_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM courses WHERE course_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id), rs.getString("course_title"), String.valueOf(rs.getInt("unit_no")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteCourse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("course_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM courses WHERE course_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("courses");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("course_id");
        String title = req.getParameter("course_title");
        String unit = req.getParameter("unit_no");

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE courses SET course_title=?, unit_no=? WHERE course_id=?")) {
                    ps.setString(1, title);
                    ps.setInt(2, unit == null || unit.isEmpty() ? 0 : Integer.parseInt(unit));
                    ps.setInt(3, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO courses (course_title, unit_no) VALUES (?, ?)")) {
                    ps.setString(1, title);
                    ps.setInt(2, unit == null || unit.isEmpty() ? 0 : Integer.parseInt(unit));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("courses");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM courses WHERE course_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
