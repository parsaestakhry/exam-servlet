package com.example.examservlet.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/index-servlet")
public class IndexServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><head><title>ExamDB</title></head><body>");
            out.println("<h1>ExamDB Portal</h1>");
            out.println("<ul>");
            out.println("<li><a href='students'>Students</a></li>");
            out.println("<li><a href='courses'>Courses</a></li>");
            out.println("<li><a href='enrollments'>Enrollments</a></li>");
            out.println("<li><a href='exams'>Exams</a></li>");
            out.println("<li><a href='questions'>Questions</a></li>");
            out.println("<li><a href='options'>Options</a></li>");
            out.println("<li><a href='studentexams'>Student Exams</a></li>");
            out.println("<li><a href='studentanswers'>Student Answers</a></li>");
            out.println("</ul>");
            out.println("</body></html>");
        }
    }
}
