package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/studentexams")
public class StudentExamsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteStudentExam(req, resp); break;
            default: listStudentExams(resp); break;
        }
    }

    private void listStudentExams(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT se.student_exam_id, se.final_result, " +
                             "s.student_code, s.first_name, s.last_name, " +
                             "e.exam_id, e.exam_title " +
                             "FROM student_exams se " +
                             "JOIN students s ON se.student_code = s.student_code " +
                             "JOIN exams e ON se.exam_id = e.exam_id " +
                             "ORDER BY se.student_exam_id")) {

            out.println("<html><body><h1>Student Exams</h1>");
            out.println("<a href='studentexams?action=new'>Add New Student Exam</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Student</th><th>Exam</th><th>Final Result</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("student_exam_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getLong("student_code") + " - " + rs.getString("first_name") + " " + rs.getString("last_name") + "</td>");
                out.println("<td>" + rs.getInt("exam_id") + " - " + rs.getString("exam_title") + "</td>");
                out.println("<td>" + rs.getString("final_result") + "</td>");
                out.println("<td><a href='studentexams?action=edit&student_exam_id=" + id + "'>Edit</a> | "
                        + "<a href='studentexams?action=delete&student_exam_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }

            out.println("</table><br><a href='/'>Back</a></body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String studentCode, String examId, String finalResult) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Student Exam Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='student_exam_id' value='" + id + "'>");
            out.println("Student Code: <input type='number' name='student_code' value='" + studentCode + "' required><br>");
            out.println("Exam ID: <input type='number' name='exam_id' value='" + examId + "' required><br>");
            out.println("Final Result: <input type='text' name='final_result' value='" + finalResult + "'><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='studentexams'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("student_exam_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM student_exams WHERE student_exam_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id),
                        String.valueOf(rs.getLong("student_code")),
                        String.valueOf(rs.getInt("exam_id")),
                        rs.getString("final_result"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteStudentExam(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("student_exam_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM student_exams WHERE student_exam_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("studentexams");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("student_exam_id");
        long studentCode = Long.parseLong(req.getParameter("student_code"));
        int examId = Integer.parseInt(req.getParameter("exam_id"));
        String finalResult = req.getParameter("final_result");

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE student_exams SET student_code=?, exam_id=?, final_result=? WHERE student_exam_id=?")) {
                    ps.setLong(1, studentCode);
                    ps.setInt(2, examId);
                    ps.setString(3, finalResult);
                    ps.setInt(4, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO student_exams (student_code, exam_id, final_result) VALUES (?, ?, ?)")) {
                    ps.setLong(1, studentCode);
                    ps.setInt(2, examId);
                    ps.setString(3, finalResult);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("studentexams");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM student_exams WHERE student_exam_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
