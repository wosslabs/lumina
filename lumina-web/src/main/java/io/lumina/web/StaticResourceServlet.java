package io.lumina.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * Serves classpath resources under {@code static/lumina-web/} at {@code GET /lumina-web/**}.
 * Task 9 adds the browser client files there; this servlet's routing does not change.
 */
final class StaticResourceServlet extends HttpServlet {
    private static final String CLASSPATH_PREFIX = "static/lumina-web";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String resourcePath = CLASSPATH_PREFIX + pathInfo;
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType(contentTypeFor(pathInfo));
            resource.transferTo(response.getOutputStream());
        }
    }

    private String contentTypeFor(String path) {
        if (path.endsWith(".js")) {
            return "text/javascript;charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css;charset=utf-8";
        }
        if (path.endsWith(".html")) {
            return "text/html;charset=utf-8";
        }
        return "application/octet-stream";
    }
}
