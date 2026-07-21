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

    private ComponentSpecs() {}
}
