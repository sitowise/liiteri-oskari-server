package fi.nls.oskari.util;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 12.6.2014
 * Time: 17:08
 * To change this template use File | Settings | File Templates.
 */
public class IOHelperTest {
    @Test
    public void testConstructUrl() throws Exception {
        String baseUrl = "/testing";
        assertEquals("Null params should return given base URL", baseUrl, IOHelper.constructUrl(baseUrl, null));

        // NOTE! using LinkedHashMap to ensure params order in result
        Map<String, String> params = new LinkedHashMap<String, String>();
        assertEquals("Empty params should return given base URL", baseUrl, IOHelper.constructUrl(baseUrl, params));

        params.put("test", "testing");
        assertEquals("Simple params should return expected URL", baseUrl + "?test=testing", IOHelper.constructUrl(baseUrl, params));

        baseUrl = "/testing?";
        assertEquals("BaseUrl with ending ? should return expected URL", baseUrl + "test=testing", IOHelper.constructUrl(baseUrl, params));

        baseUrl = "/testing?t=1";
        assertEquals("BaseUrl having existing queryString should return expected URL", baseUrl + "&test=testing", IOHelper.constructUrl(baseUrl, params));

        baseUrl = "/testing?t=1&";
        assertEquals("BaseUrl with ending & should return expected URL", baseUrl + "test=testing", IOHelper.constructUrl(baseUrl, params));

        params.put("t2", "3");
        assertEquals("Multiple params should return expected URL", baseUrl + "test=testing&t2=3", IOHelper.constructUrl(baseUrl, params));

        params.put("t3", "&&&");
        assertEquals("Problematic params should return expected encoded URL", baseUrl + "test=testing&t2=3&t3=%26%26%26", IOHelper.constructUrl(baseUrl, params));
    }
}
