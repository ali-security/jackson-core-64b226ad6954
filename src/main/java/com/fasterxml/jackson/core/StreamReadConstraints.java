package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;

public class StreamReadConstraints
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_MAX_DEPTH = 1000;

    public static final int DEFAULT_MAX_NUM_LEN = 1000;

    private static final StreamReadConstraints DEFAULT =
        new StreamReadConstraints(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NUM_LEN);

    protected final int _maxNestingDepth;
    private final int _maxNumLen;

    StreamReadConstraints(int maxNestingDepth, int maxNumLen) {
        _maxNestingDepth = maxNestingDepth;
        _maxNumLen = maxNumLen;
    }

    public static StreamReadConstraints defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxNestingDepth() {
        return _maxNestingDepth;
    }

    public int getMaxNumberLength() {
        return _maxNumLen;
    }

    public void validateNestingDepth(int depth) throws StreamConstraintsException
    {
        if (depth > _maxNestingDepth) {
            throw new StreamConstraintsException(String.format("Depth (%d) exceeds the maximum allowed nesting depth (%d)",
                    depth, _maxNestingDepth));
        }
    }

    public void validateIntegerLength(int length) throws StreamConstraintsException
    {
        if (length > _maxNumLen) {
            throw new StreamConstraintsException(
                String.format("Number value length (%d) exceeds the maximum allowed (%d)",
                    length, _maxNumLen));
        }
    }

    public void validateFPLength(int length) throws StreamConstraintsException
    {
        if (length > _maxNumLen) {
            throw new StreamConstraintsException(
                String.format("Number value length (%d) exceeds the maximum allowed (%d)",
                    length, _maxNumLen));
        }
    }

    public static final class Builder {
        private int maxNestingDepth = DEFAULT_MAX_DEPTH;
        private int maxNumLen = DEFAULT_MAX_NUM_LEN;

        Builder() { }

        public Builder maxNestingDepth(int maxNestingDepth) {
            if (maxNestingDepth < 0) {
                throw new IllegalArgumentException("Cannot set maxNestingDepth to a negative value");
            }
            this.maxNestingDepth = maxNestingDepth;
            return this;
        }

        public Builder maxNumberLength(int maxNumLen) {
            this.maxNumLen = maxNumLen;
            return this;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(maxNestingDepth, maxNumLen);
        }
    }
}
