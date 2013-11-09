package com.cloudera.flume.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.net.util.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Requests an OAuth token that can be used to open a connection to the Webtrends Streams API
 * (SAPI).
 */
public class WebtrendsSAPIAuth {
  private static final Logger LOG = Logger.getLogger(WebtrendsSAPIAuth.class);
  private static final String SECRET_KEY_TYPE = "HmacSHA256";
  private static final String CHARSET = "UTF-8";

  private static final String ENCODED_HEADER = new String(
      Base64.encodeBase64("{\"typ\":\"JWT\", \"alg\":\"HS256\"}".getBytes()));

  private static final String AUDIENCE = "auth.webtrends.com";
  private static final String SCOPE = "sapi.webtrends.com";
  private static final String AUTH_URL = "https://sauth.webtrends.com/v1/token";

  private WebtrendsSAPIAuth() {
  }

  /**
   * Requests OAuth token for the given client ID and Secret
   * @return the OAuth token
   * @throws GeneralSecurityException
   * @throws IOException
   * @throws Exception
   */
  public static String getToken(final String clientId, final String clientSecret)
      throws GeneralSecurityException, IOException {
    final String expires = String.valueOf(System.currentTimeMillis() + 60000);
    final String assertion = buildAssertion(clientId, clientSecret, expires);

    Map<String, String> requestParams = new HashMap<String, String>();
    requestParams.put("client_id", clientId);
    requestParams.put("client_assertion", URLEncoder.encode(assertion, CHARSET));

    final String result = sendRequest(requestParams);

    ObjectMapper mapper = new ObjectMapper();
    JsonFactory factory = mapper.getJsonFactory();
    JsonParser jp = factory.createJsonParser(result);
    JsonNode obj = mapper.readTree(jp);

    return obj.findValue("access_token").getValueAsText();
  }

  private static String buildAssertion(final String clientId, final String clientSecret,
      final String expires) throws GeneralSecurityException {

    StringBuilder sb = new StringBuilder();
    sb.append("{\"iss\":\"");
    sb.append(clientId);
    sb.append("\",\"prn\":\"");
    sb.append(clientId);
    sb.append("\",\"aud\":\"");
    sb.append(AUDIENCE);
    sb.append("\",\"exp\":");
    sb.append(expires);
    sb.append(",\"scope\":\"");
    sb.append(SCOPE);
    sb.append("\"}");

    if (LOG.isDebugEnabled()) {
      LOG.debug("Pre-encoded assertion: " + sb.toString());
    }

    final String message =
        ENCODED_HEADER + "." + new String(Base64.encodeBase64(sb.toString().getBytes()));
    final String signature = getHMAC256(clientSecret, message);
    return message + "." + signature;
  }

  private static String getHMAC256(String clientSecret, String input)
      throws GeneralSecurityException {
    final SecretKeySpec keySpec = new SecretKeySpec(clientSecret.getBytes(), SECRET_KEY_TYPE);
    final Mac mac = Mac.getInstance(SECRET_KEY_TYPE);
    mac.init(keySpec);
    final byte[] m = mac.doFinal(input.getBytes());
    return new String(Base64.encodeBase64(m));
  }

  private static String sendRequest(final Map<String, String> requestParams) throws IOException {
    final URL url = new URL(AUTH_URL);
    StringBuilder data = new StringBuilder();

    for (String key : requestParams.keySet()) {
      data.append(String.format("%s=%s&", key, requestParams.get(key)));
    }

    final String params = data.substring(0, data.length() - 1);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending request: " + AUTH_URL + " params:" + params);
    }

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
    connection.setDoOutput(true);

    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
    out.write(params);
    out.close();

    InputStream is =
        connection.getResponseCode() < 400 ? connection.getInputStream() : connection
            .getErrorStream();

    BufferedReader in = new BufferedReader(new InputStreamReader(is));
    String decodedString;
    StringBuffer sb = new StringBuffer();
    while ((decodedString = in.readLine()) != null) {
      sb.append(decodedString);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Response [%d]: %s", connection.getResponseCode(), sb.toString()));
    }
    in.close();

    return sb.toString();
  }
}