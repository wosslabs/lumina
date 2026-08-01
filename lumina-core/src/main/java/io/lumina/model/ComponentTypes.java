package io.lumina.model;

/**
 * Built-in component type constants shared by the wire protocol and {@code Ui} binder.
 * Each constant is the {@code type} field value on a {@link ComponentNode}.
 */
public final class ComponentTypes {
    /** Root container for the component tree. */
    public static final String ROOT = "root";
    /** Page or section heading. */
    public static final String TITLE = "title";
    /** Markdown-formatted rich text. */
    public static final String MARKDOWN = "markdown";
    /** Plain text content. */
    public static final String TEXT = "text";
    /** Clickable action control. */
    public static final String BUTTON = "button";
    /** Single-line text entry field. */
    public static final String TEXT_INPUT = "text_input";
    /** Boolean selection control. */
    public static final String CHECKBOX = "checkbox";
    /** Numeric entry control. */
    public static final String NUMBER_INPUT = "number_input";
    /** Single-choice dropdown control. */
    public static final String SELECTBOX = "selectbox";
    /** Single-choice radio group. */
    public static final String RADIO = "radio";
    /** Numeric range control. */
    public static final String SLIDER = "slider";
    /** Transient activity indicator. */
    public static final String SPINNER = "spinner";
    /** Client-side file download action. */
    public static final String DOWNLOAD_BUTTON = "download_button";
    /** Chat-style message input. */
    public static final String CHAT_INPUT = "chat_input";
    /** End-user chat message bubble. */
    public static final String USER_MESSAGE = "user_message";
    /** AI assistant chat message bubble. */
    public static final String AI_MESSAGE = "ai_message";
    /** Syntax-highlighted code block. */
    public static final String CODE = "code";
    /** Structured JSON viewer. */
    public static final String JSON = "json";
    /** Tabular data display. */
    public static final String TABLE = "table";
    /** Image display. */
    public static final String IMAGE = "image";
    /** File upload control. */
    public static final String FILE_UPLOAD = "file_upload";
    /** Progress indicator. */
    public static final String PROGRESS = "progress";
    /** Generic block container. */
    public static final String CONTAINER = "container";
    /** Row of equal-width columns. */
    public static final String COLUMNS = "columns";
    /** Single column slot inside a {@link #COLUMNS} row. */
    public static final String COLUMN = "column";
    /** Left navigation rail (at most one per build). */
    public static final String SIDEBAR = "sidebar";
    /** Collapsible section with persisted open state. */
    public static final String EXPANDER = "expander";
    /** Sidebar brand / product identity region. */
    public static final String SIDEBAR_BRAND = "sidebar_brand";
    /** Sidebar primary navigation region. */
    public static final String SIDEBAR_NAV = "sidebar_nav";
    /** Single navigable page entry inside {@link #SIDEBAR_NAV}. */
    public static final String NAV_PAGE = "nav_page";
    /** Sidebar footer / utilities region. */
    public static final String SIDEBAR_FOOTER = "sidebar_footer";
    /** Optional app header context line (not the page H1). */
    public static final String APP_HEADER = "app_header";

    private ComponentTypes() {}
}
