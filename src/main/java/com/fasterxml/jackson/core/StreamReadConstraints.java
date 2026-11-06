package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;

public class StreamReadConstraints
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_MAX_NUM_LEN = 1000;

    private static final StreamReadConstraints DEFAULT =
        new StreamReadConstraints(DEFAULT_MAX_NUM_LEN);

    private final int _maxNumLen;

    StreamReadConstraints(int maxNumLen) {
        _maxNumLen = maxNumLen;
    }

    public static StreamReadConstraints defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxNumberLength() {
        return _maxNumLen;
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
        private int maxNumLen = DEFAULT_MAX_NUM_LEN;

        Builder() { }

        public Builder maxNumberLength(int maxNumLen) {
            this.maxNumLen = maxNumLen;
            return this;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(maxNumLen);
        }
    }
}
