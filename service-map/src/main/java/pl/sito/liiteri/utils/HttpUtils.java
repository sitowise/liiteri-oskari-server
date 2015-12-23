package pl.sito.liiteri.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;

public class HttpUtils
{
	private static final Logger log = LogFactory.getLogger(HttpUtils.class);
	
//	public static String sendPost2(String url, String postData) throws Exception
//	{
//        HttpURLConnection con = null;
//        try {
//            con = IOHelper.getConnection(url);            
//            con.setRequestMethod("POST");
//            con.setDoOutput(true);
//            con.setDoInput(true);
//            HttpURLConnection.setFollowRedirects(false);
//            con.setUseCaches(false);
////            con.setRequestProperty("Accept", "application/json");
////            con.setRequestProperty(HEADER_CONTENT_TYPE, "application/json; charset=UTF-8");
//            con.connect();
//                    
//            IOHelper.writeToConnection(con, postData);
//            final String data = IOHelper.readString(con.getInputStream());
//            log.info("From server " + data);
//            return data;
//        } catch (Exception e) {
//        	log.error(e, "Cannot get token from server " + url);
//            throw e;
//        } finally {
//            try {
//                con.disconnect();
//            }
//            catch (Exception ignored) {}
//        }
//	}
	
	public static String sendPost(String url, HashMap<String, String> postData) throws IOException 
	{	
		log.info("Sending POST request to " + url);
    	
    	String responseString = null;    	    	    	  
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		if (postData != null) 
		{
			for (Entry<String, String> entry : postData.entrySet())
			{
				nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}			
		}
		
		CloseableHttpClient client = HttpClientBuilder.create().build(); 
		HttpPost request = new HttpPost(url);
		request.setEntity(new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8));
			
		try 
		{			
			CloseableHttpResponse response = client.execute(request);
			responseString = getResponseAsString(response);
		}
		catch (IOException e)
		{
			log.error(e, "Error sending POST request");
			throw e;
		}
		finally
		{
			if (client != null) 
			{
				try
				{
					client.close();
				} catch (IOException e)
				{
					/* suppress */
					log.error(e, "Cannot close HTTP client");
				}
			}				
		}
		
		return responseString;
	}
	
	public static String sendGet(String url) throws IOException 
    {
    	log.info("Sending GET request to " + url);
    	
    	String responseString = null;    	
    	
    	CloseableHttpClient client = HttpClientBuilder.create().build();    					
		
		try
		{
			CloseableHttpResponse response = client.execute(new HttpGet(url));
			responseString = getResponseAsString(response);
		} catch (IOException e)
		{
			log.error(e, "Error sending GET request");
			throw e;
		}
		finally
		{
			if (client != null) 
			{
				try
				{
					client.close();
				} catch (IOException e)
				{
					/* suppress */
					log.error(e, "Cannot close HTTP client");
				}
			}				
		}
		
		return responseString;
    }
	
	private static String getResponseAsString(CloseableHttpResponse response)
			throws IllegalStateException, IOException
	{		
		InputStream stream = response.getEntity().getContent();
		
		return IOHelper.readString(stream);
		
		//StringWriter writer = new StringWriter();
		//IOUtils.copy(stream, writer);
		//return writer.toString();
	}
}
