package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/questions")
public class QuestionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", "", "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteQuestion(req, resp); break;
            default: listQuestions(resp); break;
        }
    }

    private void listQuestions(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT q.question_id, q.question_no, q.question_description, q.score, " +
                             "e.exam_id, e.exam_title " +
                             "FROM questions q JOIN exams e ON q.exam_id = e.exam_id " +
                             "ORDER BY q.exam_id, q.question_no")) {

            out.println("<html><body><h1>Questions</h1>");
            out.println("<a href='questions?action=new'>Add New Question</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Exam</th><th>No</th><th>Description</th><th>Score</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("question_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getInt("exam_id") + " - " + rs.getString("exam_title") + "</td>");
                out.println("<td>" + rs.getInt("question_no") + "</td>");
                out.println("<td>" + rs.getString("question_description") + "</td>");
                out.println("<td>" + rs.getInt("score") + "</td>");
                out.println("<td><a href='questions?action=edit&question_id=" + id + "'>Edit</a> | "
                        + "<a href='questions?action=delete&question_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }

            out.println("</table><br><a href='/'>Back</a></body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String examId, String no, String desc, String score) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Question Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='question_id' value='" + id + "'>");
            out.println("Exam ID: <input type='number' name='exam_id' value='" + examId + "' required><br>");
            out.println("Question No: <input type='number' name='question_no' value='" + no + "' required><br>");
            out.println("Description: <textarea name='question_description' required>" + desc + "</textarea><br>");
            out.println("Score: <input type='number' name='score' value='" + (score == null || score.isEmpty() ? "1" : score) + "' required><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='questions'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("question_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM questions WHERE question_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id),
                        String.valueOf(rs.getInt("exam_id")),
                        String.valueOf(rs.getInt("question_no")),
                        rs.getString("question_description"),
                        String.valueOf(rs.getInt("score")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteQuestion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("question_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM questions WHERE question_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("questions");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("question_id");
        int examId = Integer.parseInt(req.getParameter("exam_id"));
        int no = Integer.parseInt(req.getParameter("question_no"));
        String desc = req.getParameter("question_description");
        int score = Integer.parseInt(req.getParameter("score"));

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE questions SET exam_id=?, question_no=?, question_description=?, score=? WHERE question_id=?")) {
                    ps.setInt(1, examId);
                    ps.setInt(2, no);
                    ps.setString(3, desc);
                    ps.setInt(4, score);
                    ps.setInt(5, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO questions (exam_id, question_no, question_description, score) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, examId);
                    ps.setInt(2, no);
                    ps.setString(3, desc);
                    ps.setInt(4, score);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("questions");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM questions WHERE question_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
