package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/students")
public class StudentsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");

        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new":
                showNewForm(resp);
                break;
            case "edit":
                showEditForm(req, resp);
                break;
            case "delete":
                deleteStudent(req, resp);
                break;
            default:
                listStudents(resp);
                break;
        }
    }

    private void listStudents(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students ORDER BY student_code")) {

            out.println("<html><head><title>Students</title></head><body>");
            out.println("<h1>Students List</h1>");
            out.println("<a href='students?action=new'>Add New Student</a><br><br>");
            out.println("<table border='1'><tr><th>Code</th><th>First Name</th><th>Last Name</th><th>Field</th><th>Actions</th></tr>");

            while (rs.next()) {
                long code = rs.getLong("student_code");
                out.println("<tr>");
                out.println("<td>" + code + "</td>");
                out.println("<td>" + rs.getString("first_name") + "</td>");
                out.println("<td>" + rs.getString("last_name") + "</td>");
                out.println("<td>" + rs.getString("field_name") + "</td>");
                out.println("<td>"
                        + "<a href='students?action=edit&student_code=" + code + "'>Edit</a> | "
                        + "<a href='students?action=delete&student_code=" + code + "' onclick='return confirm(\"Are you sure?\")'>Delete</a>"
                        + "</td>");
                out.println("</tr>");
            }

            out.println("</table>");
            out.println("</body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showNewForm(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body>");
            out.println("<h1>Add New Student</h1>");
            out.println("<form method='post' action='students'>");
            out.println("Code: <input type='number' name='student_code' required><br>");
            out.println("First Name: <input type='text' name='first_name' required><br>");
            out.println("Last Name: <input type='text' name='last_name' required><br>");
            out.println("Field: <input type='text' name='field_name'><br>");
            out.println("<input type='submit' value='Save'>");
            out.println("</form>");
            out.println("<a href='students'>Cancel</a>");
            out.println("</body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long code = Long.parseLong(req.getParameter("student_code"));

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM students WHERE student_code = ?")) {

            ps.setLong(1, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<html><body>");
                    out.println("<h1>Edit Student</h1>");
                    out.println("<form method='post' action='students'>");
                    out.println("<input type='hidden' name='student_code' value='" + rs.getLong("student_code") + "'>");
                    out.println("First Name: <input type='text' name='first_name' value='" + rs.getString("first_name") + "' required><br>");
                    out.println("Last Name: <input type='text' name='last_name' value='" + rs.getString("last_name") + "' required><br>");
                    out.println("Field: <input type='text' name='field_name' value='" + rs.getString("field_name") + "'><br>");
                    out.println("<input type='submit' value='Update'>");
                    out.println("</form>");
                    out.println("<a href='students'>Cancel</a>");
                    out.println("</body></html>");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteStudent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long code = Long.parseLong(req.getParameter("student_code"));

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_code = ?")) {
            ps.setLong(1, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("students");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String studentCodeParam = req.getParameter("student_code");
        String firstName = req.getParameter("first_name");
        String lastName = req.getParameter("last_name");
        String fieldName = req.getParameter("field_name");

        try (Connection conn = DbUtil.getConnection()) {
            if (isStudentExisting(conn, studentCodeParam)) {
                // Update existing
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE students SET first_name=?, last_name=?, field_name=? WHERE student_code=?")) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, fieldName);
                    ps.setLong(4, Long.parseLong(studentCodeParam));
                    ps.executeUpdate();
                }
            } else {
                // Insert new
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO students (student_code, first_name, last_name, field_name) VALUES (?, ?, ?, ?)")) {
                    ps.setLong(1, Long.parseLong(studentCodeParam));
                    ps.setString(2, firstName);
                    ps.setString(3, lastName);
                    ps.setString(4, fieldName);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("students");
    }

    private boolean isStudentExisting(Connection conn, String studentCodeParam) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM students WHERE student_code = ?")) {
            ps.setLong(1, Long.parseLong(studentCodeParam));
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }
}
