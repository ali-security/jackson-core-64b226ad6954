package com.fasterxml.jackson.core.io;

/**
 * Simple container for a reference to parser (or generator) input source
 * (or target), along with the offset and length of the window within that
 * source the caller actually handed over.
 *<p>
 * Needed so that content snippets included in {@link
 * com.fasterxml.jackson.core.JsonLocation} descriptions can be limited to
 * the caller-declared window instead of exposing the whole backing buffer.
 *
 * @since 2.9.6-sp (backport of 2.13 {@code ContentReference})
 */
public class ContentReference
{
    /**
     * Actual source reference: {@code byte[]}, {@code char[]},
     * {@link CharSequence}, {@link java.io.InputStream} and so on.
     */
    private final Object _rawContent;

    /**
     * Offset of the first content unit within {@link #_rawContent}, if known;
     * {@code -1} if not applicable/available.
     */
    private final int _offset;

    /**
     * Length of relevant content within {@link #_rawContent}, if known;
     * {@code -1} if not applicable/available.
     */
    private final int _length;

    public ContentReference(Object rawContent, int offset, int length) {
        _rawContent = rawContent;
        _offset = offset;
        _length = length;
    }

    public ContentReference(Object rawContent) {
        this(rawContent, -1, -1);
    }

    public Object getRawContent() {
        return _rawContent;
    }

    public int contentOffset() {
        return _offset;
    }

    public int contentLength() {
        return _length;
    }

    /**
     * @return {@code true} if content is textual (or textually decodable) and
     *   may hence be included, in part, in location descriptions
     */
    public boolean hasTextualContent() {
        return (_rawContent instanceof CharSequence)
                || (_rawContent instanceof char[])
                || (_rawContent instanceof byte[]);
    }
}
