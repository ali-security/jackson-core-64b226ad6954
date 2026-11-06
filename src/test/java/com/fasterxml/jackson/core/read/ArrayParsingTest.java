package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Tests for array parsing
 */
public class ArrayParsingTest extends BaseTest
{
    public void testDeepNesting() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        JsonParser jp = createParserUsingStream(new JsonFactory(), DOC, "UTF-8");
        try {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
        jp.close();
    }

    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }
}
