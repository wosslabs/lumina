package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.LAYOUT;
import static io.lumina.components.ComponentSpecs.PAGE_TITLE;
import static io.lumina.components.ComponentSpecs.SIDEBAR_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.LuminaException;
import io.lumina.model.ComponentNode;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.SidebarState;
import org.junit.jupiter.api.Test;

class UiBinderPageConfigTest {

    @Test
    void pageConfigEmitsRootProps() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.pageConfig(PageConfig.builder()
                .title("Dashboard")
                .layout(PageLayout.CENTERED)
                .sidebar(SidebarState.COLLAPSED)
                .build());
        ui.title("Hello");

        ComponentNode root = ui.buildRoot();
        assertThat(root.props())
                .containsEntry(PAGE_TITLE, "Dashboard")
                .containsEntry(LAYOUT, "centered")
                .containsEntry(SIDEBAR_STATE, "collapsed");
    }

    @Test
    void pageConfigMustBeFirstCall() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.text("too early");
        assertThatThrownBy(() -> ui.pageConfig(PageConfig.builder().build()))
                .isInstanceOf(LuminaException.class)
                .hasMessageContaining("first");
    }
}
