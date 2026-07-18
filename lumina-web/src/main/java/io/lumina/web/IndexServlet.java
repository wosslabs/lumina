package io.lumina.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * Serves the placeholder client shell at {@code GET /}. Task 9 replaces {@code static/index.html}
 * with the full browser client shell; this servlet's routing does not change.
 */
final class IndexServlet extends HttpServlet {
    private static final String RESOURCE = "static/index.html";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            if (resource == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("text/html;charset=utf-8");
            resource.transferTo(response.getOutputStream());
        }
    }
}
