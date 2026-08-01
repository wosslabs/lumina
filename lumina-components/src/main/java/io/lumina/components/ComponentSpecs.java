package io.lumina.components;

/**
 * Property names shared by component tree producers and renderers.
 */
public final class ComponentSpecs {
    /** Property containing display text or rich content. */
    public static final String CONTENT = "content";
    /** Property containing a control's visible label. */
    public static final String LABEL = "label";
    /** Property containing a control or display value. */
    public static final String VALUE = "value";
    /** Property containing a numeric control's inclusive minimum. */
    public static final String MIN = "min";
    /** Property containing a numeric control's inclusive maximum. */
    public static final String MAX = "max";
    /** Property containing a numeric control's increment. */
    public static final String STEP = "step";
    /** Property containing a single-choice control's available values. */
    public static final String OPTIONS = "options";
    /** Property containing a download file name. */
    public static final String FILENAME = "fileName";
    /** Property containing base64-encoded download data. */
    public static final String DATA = "data";
    /** Property containing an activity indicator's state. */
    public static final String ACTIVE = "active";
    /** Property containing a code block's language identifier. */
    public static final String LANGUAGE = "language";
    /** Property containing code block source text. */
    public static final String SOURCE = "source";
    /** Property containing tabular row data. */
    public static final String ROWS = "rows";
    /** Property containing an image URL or resource path. */
    public static final String SRC = "src";
    /** Property containing the number of columns in a {@code columns} node. */
    public static final String COUNT = "count";
    /** Property containing a column's zero-based index. */
    public static final String INDEX = "index";
    /** Property containing whether an expander is open. */
    public static final String OPEN = "open";
    /** Property containing the browser tab title for the page. */
    public static final String PAGE_TITLE = "pageTitle";
    /** Property containing the main content layout mode. */
    public static final String LAYOUT = "layout";
    /** Property containing the sidebar visual state. */
    public static final String SIDEBAR_STATE = "sidebarState";

    /** Property containing the server-side route path for this session. */
    public static final String PATH = "path";

    private ComponentSpecs() {}
}
