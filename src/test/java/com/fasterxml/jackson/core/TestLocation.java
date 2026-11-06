package com.fasterxml.jackson.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.core.io.ContentReference;

public class TestLocation extends BaseTest
{
    static class Foobar { }

    public void testBasics()
    {
        JsonLocation loc1 = new JsonLocation("src", 10L, 10L, 1, 2);
        JsonLocation loc2 = new JsonLocation(null, 10L, 10L, 3, 2);
        assertEquals(loc1, loc1);
        assertFalse(loc1.equals(null));
        assertFalse(loc1.equals(loc2));
        assertFalse(loc2.equals(loc1));

        // don't care about what it is; should not compute to 0 with data above
        assertTrue(loc1.hashCode() != 0);
        assertTrue(loc2.hashCode() != 0);
    }

    public void testBasicToString() throws Exception
    {
        // no location:
        assertEquals("[Source: UNKNOWN; line: 3, column: 2]",
                new JsonLocation(null, 10L, 10L, 3, 2).toString());

        // Short String
        assertEquals("[Source: (String)\"string-source\"; line: 1, column: 2]",
                new JsonLocation("string-source", 10L, 10L, 1, 2).toString());

        // Short char[]
        assertEquals("[Source: (char[])\"chars-source\"; line: 1, column: 2]",
                new JsonLocation("chars-source".toCharArray(), 10L, 10L, 1, 2).toString());

        // Short byte[]
        assertEquals("[Source: (byte[])\"bytes-source\"; line: 1, column: 2]",
                new JsonLocation("bytes-source".getBytes("UTF-8"), 10L, 10L, 1, 2).toString());

        // InputStream
        assertEquals("[Source: (ByteArrayInputStream); line: 1, column: 2]",
                new JsonLocation(new ByteArrayInputStream(new byte[0]), 10L, 10L, 1, 2).toString());

        // Class<?> that specifies source type
        assertEquals("[Source: (InputStream); line: 1, column: 2]",
                new JsonLocation(InputStream.class, 10L, 10L, 1, 2).toString());

        // misc other
        Foobar srcRef = new Foobar();
        assertEquals("[Source: ("+srcRef.getClass().getName()+"); line: 1, column: 2]",
                new JsonLocation(srcRef, 10L, 10L, 1, 2).toString());
    }

    public void testTruncatedSource() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JsonLocation.MAX_CONTENT_SNIPPET; ++i) {
            sb.append("x");
        }
        String main = sb.toString();
        String json = main + "yyy";
        JsonLocation loc = new JsonLocation(json, 0L, 0L, 1, 1);
        String desc = loc.sourceDescription();
        assertEquals(String.format("(String)\"%s\"[truncated 3 chars]", main), desc);

        // and same with bytes
        loc = new JsonLocation(json.getBytes("UTF-8"), 0L, 0L, 1, 1);
        desc = loc.sourceDescription();
        assertEquals(String.format("(byte[])\"%s\"[truncated 3 bytes]", main), desc);
    }

    // [core#652]: when source is given as an offset/length window, snippet must
    // cover only that window, and the "[truncated N]" count must be relative to
    // the window length -- not to the full backing buffer
    public void testTruncatedSourceWithOffset() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JsonLocation.MAX_CONTENT_SNIPPET; ++i) {
            sb.append("x");
        }
        final String main = sb.toString();
        final String window = main + "yyy";
        final String prefix = "SECRET-BEFORE";
        final String doc = prefix + window + "SECRET-AFTER";
        final int offset = prefix.length();
        final int len = window.length();

        // char[]-backed window
        JsonLocation loc = new JsonLocation(
                new ContentReference(doc.toCharArray(), offset, len), 0L, 0L, 1, 1);
        assertEquals(String.format("(char[])\"%s\"[truncated 3 chars]", main),
                loc.sourceDescription());

        // byte[]-backed window
        loc = new JsonLocation(
                new ContentReference(doc.getBytes("UTF-8"), offset, len), 0L, 0L, 1, 1);
        assertEquals(String.format("(byte[])\"%s\"[truncated 3 bytes]", main),
                loc.sourceDescription());

        // CharSequence-backed window
        loc = new JsonLocation(new ContentReference(doc, offset, len), 0L, 0L, 1, 1);
        assertEquals(String.format("(String)\"%s\"[truncated 3 chars]", main),
                loc.sourceDescription());
    }

    // [core#652]: window shorter than the buffer must not spill; also verify
    // out-of-range/unknown markers degrade safely instead of dumping content
    public void testSourceWindowEdgeCases() throws Exception
    {
        final String doc = "PREFIX[broken]SUFFIX";
        final byte[] docB = doc.getBytes("UTF-8");

        // exact window
        assertEquals("(byte[])\"[broken]\"",
                new JsonLocation(new ContentReference(docB, 6, 8), 0L, 0L, 1, 1)
                    .sourceDescription());

        // length longer than what remains: clamped to the array, no exception
        // (trailing "[truncated ...]" marker reflects the over-declared length)
        String desc = new JsonLocation(new ContentReference(docB, 6, 9999), 0L, 0L, 1, 1)
                .sourceDescription();
        assertTrue("Unexpected description: "+desc,
                desc.startsWith("(byte[])\"[broken]SUFFIX\""));

        // offset past end of buffer: no content shown
        desc = new JsonLocation(new ContentReference(docB, 9999, 8), 0L, 0L, 1, 1)
                .sourceDescription();
        assertTrue("Unexpected description: "+desc, desc.startsWith("(byte[])\"\""));

        // "unknown" markers (-1/-1): no content exposed at all
        assertEquals("(byte[])\"\"",
                new JsonLocation(new ContentReference(docB), 0L, 0L, 1, 1)
                    .sourceDescription());

        // and raw (unwrapped) sources keep the legacy full-content behavior
        assertEquals("(byte[])\""+doc+"\"",
                new JsonLocation(docB, 0L, 0L, 1, 1).sourceDescription());

        // finally: getSourceRef() still hands back the raw content, not the wrapper
        assertSame(docB, new JsonLocation(new ContentReference(docB, 6, 8),
                0L, 0L, 1, 1).getSourceRef());
    }

    // for [jackson-core#356]
    public void testDisableSourceInclusion() throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.disable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);

        JsonParser p = f.createParser("[ foobar ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.getSourceRef());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();

        // and verify same works for byte-based too
        p = f.createParser("[ foobar ]".getBytes("UTF-8"));
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Shouldn't have passed");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized token");
            JsonLocation loc = e.getLocation();
            assertNull(loc.getSourceRef());
            assertEquals("UNKNOWN", loc.sourceDescription());
        }
        p.close();
    }
}
