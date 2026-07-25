package io.lumina.ui;

/** Streamlit {@code set_page_config} equivalent — must be the first {@link Ui} call per build. */
public record PageConfig(String title, PageLayout layout, SidebarState sidebarState) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "";
        private PageLayout layout = PageLayout.WIDE;
        private SidebarState sidebarState = SidebarState.EXPANDED;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder layout(PageLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder sidebar(SidebarState sidebarState) {
            this.sidebarState = sidebarState;
            return this;
        }

        public PageConfig build() {
            return new PageConfig(title, layout, sidebarState);
        }
    }
}
