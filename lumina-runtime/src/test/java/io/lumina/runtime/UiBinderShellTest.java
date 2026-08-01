package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.model.ComponentNode;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.PageConfig;
import org.junit.jupiter.api.Test;

class UiBinderShellTest {

    @Test
    void structuredSidebarEmitsBrandNavFooterAndNavPages() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.pageConfig(PageConfig.builder().title("App").build());
        ui.sidebar(sb -> {
            sb.brand(b -> b.text("Brand"));
            sb.nav(nav -> {
                nav.page("Home", "/");
                nav.page("About", "/about");
            });
            sb.footer(f -> f.text("Foot"));
        });
        ui.header(h -> h.title("Context"));
        ui.title("Page");

        ComponentNode root = ui.buildRoot();
        assertThat(root.children()).anyMatch(n -> "app_header".equals(n.type()));
        ComponentNode sidebar = root.children().stream()
                .filter(n -> "sidebar".equals(n.type()))
                .findFirst()
                .orElseThrow();
        assertThat(sidebar.children())
                .extracting(ComponentNode::type)
                .contains("sidebar_brand", "sidebar_nav", "sidebar_footer");
        ComponentNode nav = sidebar.children().stream()
                .filter(n -> "sidebar_nav".equals(n.type()))
                .findFirst()
                .orElseThrow();
        assertThat(nav.children()).allMatch(n -> "nav_page".equals(n.type()));
        assertThat(nav.children().getFirst().props())
                .containsEntry("label", "Home")
                .containsEntry("path", "/");
    }

    @Test
    void legacyFreeformSidebarStillWorks() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.sidebar(sb -> sb.button("Only"));
        ComponentNode sidebar = ui.buildRoot().children().getFirst();
        assertThat(sidebar.type()).isEqualTo("sidebar");
        assertThat(sidebar.children().getFirst().type()).isEqualTo("button");
    }
}
