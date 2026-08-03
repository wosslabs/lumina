package io.lumina.web;

import io.lumina.plugin.ExtensionRegistry;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the browser client shell at {@code GET /}, injecting ThemeSpi stylesheet links after the
 * base Lumina CSS.
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
            String html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder links = new StringBuilder();
            for (String href : ExtensionRegistry.themeCssResources()) {
                if (href == null || href.isBlank()) {
                    continue;
                }
                String safe = href.startsWith("/") ? href : "/" + href;
                links.append("  <link rel=\"stylesheet\" href=\"").append(safe).append("\" />\n");
            }
            html = html.replace("</head>", links + "</head>");
            response.setContentType("text/html;charset=utf-8");
            response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
        }
    }
}
