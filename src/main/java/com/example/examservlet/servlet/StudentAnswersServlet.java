package com.example.examservlet.servlet;

import com.example.examservlet.db.DbUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/studentanswers")
public class StudentAnswersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new": showForm(resp, null, "", "", "", ""); break;
            case "edit": showEditForm(req, resp); break;
            case "delete": deleteAnswer(req, resp); break;
            default: listAnswers(resp); break;
        }
    }

    private void listAnswers(HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter();
             Connection conn = DbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT sa.student_answer_id, sa.score_given, " +
                             "se.student_exam_id, s.student_code, s.first_name, s.last_name, " +
                             "e.exam_id, e.exam_title, " +
                             "q.question_id, q.question_description, " +
                             "o.option_id, o.option_title " +
                             "FROM student_answers sa " +
                             "JOIN student_exams se ON sa.student_exam_id = se.student_exam_id " +
                             "JOIN students s ON se.student_code = s.student_code " +
                             "JOIN exams e ON se.exam_id = e.exam_id " +
                             "JOIN questions q ON sa.question_id = q.question_id " +
                             "LEFT JOIN options o ON sa.chosen_option_id = o.option_id " +
                             "ORDER BY sa.student_answer_id")) {

            out.println("<html><body><h1>Student Answers</h1>");
            out.println("<a href='studentanswers?action=new'>Add New Answer</a><br><br>");
            out.println("<table border='1'><tr><th>ID</th><th>Student Exam</th><th>Question</th><th>Chosen Option</th><th>Score Given</th><th>Actions</th></tr>");

            while (rs.next()) {
                int id = rs.getInt("student_answer_id");
                out.println("<tr><td>" + id + "</td>");
                out.println("<td>" + rs.getInt("student_exam_id") + " - " + rs.getLong("student_code") + " " + rs.getString("first_name") + "</td>");
                out.println("<td>" + rs.getInt("question_id") + " - " + rs.getString("question_description") + "</td>");
                out.println("<td>" + (rs.getInt("option_id") != 0 ? rs.getString("option_title") : "") + "</td>");
                out.println("<td>" + rs.getBigDecimal("score_given") + "</td>");
                out.println("<td><a href='studentanswers?action=edit&student_answer_id=" + id + "'>Edit</a> | "
                        + "<a href='studentanswers?action=delete&student_answer_id=" + id + "' onclick='return confirm(\"Are you sure?\")'>Delete</a></td></tr>");
            }

            out.println("</table><br><a href='/'>Back</a></body></html>");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void showForm(HttpServletResponse resp, String id, String studentExamId, String questionId, String chosenOptionId, String scoreGiven) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body><h1>Student Answer Form</h1>");
            out.println("<form method='post'>");
            if (id != null) out.println("<input type='hidden' name='student_answer_id' value='" + id + "'>");
            out.println("Student Exam ID: <input type='number' name='student_exam_id' value='" + studentExamId + "' required><br>");
            out.println("Question ID: <input type='number' name='question_id' value='" + questionId + "' required><br>");
            out.println("Chosen Option ID: <input type='number' name='chosen_option_id' value='" + chosenOptionId + "'><br>");
            out.println("Score Given: <input type='number' step='0.01' name='score_given' value='" + (scoreGiven == null || scoreGiven.isEmpty() ? "0.00" : scoreGiven) + "'><br>");
            out.println("<input type='submit' value='Save'></form>");
            out.println("<a href='studentanswers'>Cancel</a></body></html>");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("student_answer_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM student_answers WHERE student_answer_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                showForm(resp, String.valueOf(id),
                        String.valueOf(rs.getInt("student_exam_id")),
                        String.valueOf(rs.getInt("question_id")),
                        rs.getInt("chosen_option_id") != 0 ? String.valueOf(rs.getInt("chosen_option_id")) : "",
                        rs.getBigDecimal("score_given").toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteAnswer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("student_answer_id"));
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM student_answers WHERE student_answer_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.sendRedirect("studentanswers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String idParam = req.getParameter("student_answer_id");
        int studentExamId = Integer.parseInt(req.getParameter("student_exam_id"));
        int questionId = Integer.parseInt(req.getParameter("question_id"));
        String chosenOptionIdParam = req.getParameter("chosen_option_id");
        Integer chosenOptionId = (chosenOptionIdParam == null || chosenOptionIdParam.isEmpty()) ? null : Integer.parseInt(chosenOptionIdParam);
        String scoreParam = req.getParameter("score_given");
        java.math.BigDecimal scoreGiven = (scoreParam == null || scoreParam.isEmpty()) ? new java.math.BigDecimal("0.00") : new java.math.BigDecimal(scoreParam);

        try (Connection conn = DbUtil.getConnection()) {
            if (idParam != null && exists(conn, idParam)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE student_answers SET student_exam_id=?, question_id=?, chosen_option_id=?, score_given=? WHERE student_answer_id=?")) {
                    ps.setInt(1, studentExamId);
                    ps.setInt(2, questionId);
                    if (chosenOptionId != null) ps.setInt(3, chosenOptionId); else ps.setNull(3, java.sql.Types.INTEGER);
                    ps.setBigDecimal(4, scoreGiven);
                    ps.setInt(5, Integer.parseInt(idParam));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO student_answers (student_exam_id, question_id, chosen_option_id, score_given) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, studentExamId);
                    ps.setInt(2, questionId);
                    if (chosenOptionId != null) ps.setInt(3, chosenOptionId); else ps.setNull(3, java.sql.Types.INTEGER);
                    ps.setBigDecimal(4, scoreGiven);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        resp.sendRedirect("studentanswers");
    }

    private boolean exists(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM student_answers WHERE student_answer_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeQuery().next();
        }
    }
}
