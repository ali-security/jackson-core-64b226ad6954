package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

/**
 * Test size of parser error messages
 */
public class TestMaxErrorSize
    extends com.fasterxml.jackson.core.BaseTest
{
    private final static int EXPECTED_MAX_TOKEN_LEN = 256; // ParserBase.MAX_ERROR_TOKEN_LENGTH
    
    private final JsonFactory JSON_F = new JsonFactory();

    public void testLongErrorMessage() throws Exception
    {
        _testLongErrorMessage(MODE_INPUT_STREAM);
        _testLongErrorMessage(MODE_INPUT_STREAM_THROTTLED);
    }

    public void testLongErrorMessageReader() throws Exception
    {
_testLongErrorMessage(MODE_READER);
    }

    private void _testLongErrorMessage(int mode) throws Exception
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertTrue(DOC.length() > 256);
        JsonParser jp = createParser(JSON_F, mode, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
        	String msg = jpe.getMessage();
        	final String expectedPrefix = "Unrecognized token '";
        	final String expectedSuffix = "...': was expecting ('true', 'false' or 'null')";
        	assertTrue(msg.startsWith(expectedPrefix));
        	assertTrue(msg.contains(expectedSuffix));
          int tokenLen = msg.indexOf (expectedSuffix) - expectedPrefix.length();
        	assertEquals(EXPECTED_MAX_TOKEN_LEN, tokenLen);
        }
        jp.close();
    }

    public void testShortErrorMessage() throws Exception
    {
        _testShortErrorMessage(MODE_INPUT_STREAM);
        _testShortErrorMessage(MODE_INPUT_STREAM_THROTTLED);
        _testShortErrorMessage(MODE_READER);
    }

    public void _testShortErrorMessage(int mode) throws Exception
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertTrue(DOC.length() < 256);
        JsonParser jp = createParser(JSON_F, mode, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
            String msg = jpe.getMessage();
            final String expectedPrefix = "Unrecognized token '";
            final String expectedSuffix = "': was expecting ('true', 'false' or 'null')";
            assertTrue(msg.startsWith(expectedPrefix));
            assertTrue(msg.contains(expectedSuffix));
            int tokenLen = msg.indexOf (expectedSuffix) - expectedPrefix.length();
            assertEquals(DOC.length(), tokenLen);
        }
        jp.close();
    }

    // [core#1698]: every parser backend must honor max error token length;
    //   `UTF8DataInputJsonParser` used to accumulate the whole token instead
    public void testErrorTokenLengthBoundedInAllModes() throws Exception
    {
        final int maxLen = EXPECTED_MAX_TOKEN_LEN;
        // Broken token far longer than the limit: must be truncated, not accumulated in full
        final String doc = _buildBrokenJsonOfLength(50 * maxLen);
        // limit, plus appended "..."
        final String expToken = _brokenToken(maxLen) + "...";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_F, mode, doc);
            try {
                p.nextToken();
                p.nextToken();
                fail("Should not pass, mode: "+mode);
            } catch (JsonProcessingException e) {
                assertEquals("mode: "+mode, expToken, _unrecognizedToken(e.getMessage()));
            } finally {
                p.close();
            }
        }
    }

    // [core#1698]: same bound must apply when the broken token starts with a
    //   (partially or fully) matched keyword, which takes a different error path
    public void testErrorTokenAfterKeywordPrefixBoundedInAllModes() throws Exception
    {
        final int maxLen = EXPECTED_MAX_TOKEN_LEN;
        final String longTail = _brokenToken(50 * maxLen);
        // fully matched keyword, followed by identifier chars
        _testErrorTokenAfterKeywordPrefix("true" + longTail, maxLen);
        // partially matched keyword (mismatch on 3rd char)
        _testErrorTokenAfterKeywordPrefix("nu" + longTail, maxLen);
    }

    private void _testErrorTokenAfterKeywordPrefix(String token, int maxLen) throws Exception
    {
        final String doc = "{\"key\":" + token + "!}";
        final String expToken = token.substring(0, maxLen) + "...";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_F, mode, doc);
            try {
                p.nextToken();
                p.nextToken();
                p.nextToken();
                fail("Should not pass, mode: "+mode);
            } catch (JsonProcessingException e) {
                assertEquals("mode: "+mode, expToken, _unrecognizedToken(e.getMessage()));
            } finally {
                p.close();
            }
        }
    }

    // Short broken token (below limit) must be reported verbatim, without
    // duplicated leading char, in all modes
    public void testShortErrorTokenReportedExactlyInAllModes() throws Exception
    {
        final String doc = "{\"key\":abc!}";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(JSON_F, mode, doc);
            try {
                p.nextToken();
                p.nextToken();
                fail("Should not pass, mode: "+mode);
            } catch (JsonProcessingException e) {
                assertEquals("mode: "+mode, "abc", _unrecognizedToken(e.getMessage()));
            } finally {
                p.close();
            }
        }
    }

    // Extracts X from "Unrecognized token 'X': was expecting ..."
    private String _unrecognizedToken(String msg)
    {
        final String prefix = "Unrecognized token '";
        final int start = msg.indexOf(prefix);
        assertTrue("message: "+msg, start >= 0);
        final int end = msg.indexOf("': was expecting", start);
        assertTrue("message: "+msg, end > start);
        return msg.substring(start + prefix.length(), end);
    }

    private String _buildBrokenJsonOfLength(int len)
    {
        return "{\"key\":" + _brokenToken(len) + "!}";
    }

    // Varied (not repeating single) chars so that duplicated/dropped chars are detectable
    private String _brokenToken(int len)
    {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }
}

