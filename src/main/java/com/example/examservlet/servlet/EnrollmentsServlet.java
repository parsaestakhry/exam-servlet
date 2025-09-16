package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/enrollments")
public class EnrollmentsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        System.out.println("Enrollments called !!!!!!!!!!");
        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, null); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteEnrollment(req, resp); break;
            default: listEnrollments(resp); break;
        }
    }

    private void listEnrollments(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT e.student_code, s.first_name, s.last_name, e.course_id, c.course_title " +
                             "FROM enrollments e " +
                             "JOIN students s ON e.student_code = s.student_code " +
                             "JOIN courses c ON e.course_id = c.course_id " +
                             "ORDER BY e.student_code, e.course_id")) {

            out.println("<html><body><h1>Enrollments</h1>");
            out.println("<a href='enrollments?action=new'>Add New Enrollment</a><br><br>");
            out.println("<table border='1'><tr><th>Student</th><th>Course</th><th>Actions</th></tr>");

            while (rs.next()) {
                long studentCode = rs.getLong("student_code");
                int courseId = rs.getInt("course_id");
                out.println("<tr><td>" + studentCode + " - " + rs.getString("first_name") + " " + rs.getString("last_name") + "</td>");
                out.println("<td>" + courseId + " - " + rs.getString("course_title") + "</td>");
                out.println("<td><a href='enrollments?action=edit&student_code=" + studentCode + "&course_id=" + courseId + "'>Edit</a> | "
                        + "<a href='enrollments?action=delete&student_code=" + studentCode + "&course_id=" + courseId + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }

            out.println("</table><br><a href='/'>Back</a></body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String studentCode, String courseId) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Enrollment Form</h1>");
            out.println("<form method='post'>");

            if (studentCode != null && courseId != null) {
                out.println("<input type='hidden' name='student_code' value='" + studentCode + "'>");
                out.println("<input type='hidden' name='course_id' value='" + courseId + "'>");
                out.println("Student Code: " + studentCode + "<br>");
                out.println("Course ID: " + courseId + "<br>");
            } else {
                out.println("Student Code: <input type='number' name='student_code' required><br>");
                out.println("Course ID: <input type='number' name='course_id' required><br>");
            }

            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='enrollments'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentCode = req.getParameter("student_code");
        String courseId = req.getParameter("course_id");
        showForm(resp, studentCode, courseId);
    }

    private void deleteEnrollment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long studentCode = Long.parseLong(req.getParameter("student_code"));
        int courseId = Integer.parseInt(req.getParameter("course_id"));

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM enrollments WHERE student_code=? AND course_id=?")) {
            ps.setLong(1, studentCode);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("enrollments");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long studentCode = Long.parseLong(req.getParameter("student_code"));
        int courseId = Integer.parseInt(req.getParameter("course_id"));

        try (Connection conn = DbUtil.getConnection()) {
            if (!exists(conn, studentCode, courseId)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO enrollments (student_code, course_id) VALUES (?, ?)")) {
                    ps.setLong(1, studentCode);
                    ps.setInt(2, courseId);
                    ps.executeUpdate();
                }
            }
            // no update logic because PK can't be changed
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("enrollments");
    }

    private boolean exists(Connection conn, long studentCode, int courseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM enrollments WHERE student_code=? AND course_id=?")) {
            ps.setLong(1, studentCode);
            ps.setInt(2, courseId);
            return ps.executeQuery().next();
        }
    }
}
