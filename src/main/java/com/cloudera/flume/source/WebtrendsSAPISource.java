package com.cloudera.flume.source;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.interceptor.TimestampInterceptor;
import org.apache.flume.source.AbstractSource;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * A Flume source, which pulls data from the Webtrends streaming API (SAPI).
 */
public class WebtrendsSAPISource extends AbstractSource implements EventDrivenSource, Configurable {
  private static final Logger LOG = Logger.getLogger(WebtrendsSAPISource.class);
  private static final String CLIENT_ID = "clientId";
  private static final String CLIENT_SECRET = "clientSecret";
  private static final String SAPI_URI = "sapiURL";
  private static final String SAPI_TYPE = "sapiStreamType";
  private static final String SAPI_QUERY = "sapiStreamQuery";
  private static final String SAPI_VERSION = "sapiVersion";
  private static final String SAPI_SCHEMA_VERSION = "sapiSchemaVersion";

  private WebSocketClient client;
  private String oAuthToken;
  private String streamURI;
  private String streamType;
  private String streamQuery;
  private String streamVersion;
  private String streamSchemaVersion;

  /** {@inheritDoc} */
  @Override
  public void configure(Context context) {
    final String clientId = context.getString(CLIENT_ID);
    final String clientSecret = context.getString(CLIENT_SECRET);
    streamURI = context.getString(SAPI_URI);
    streamType = context.getString(SAPI_TYPE);
    streamQuery = context.getString(SAPI_QUERY);
    streamVersion = context.getString(SAPI_VERSION);
    streamSchemaVersion = context.getString(SAPI_SCHEMA_VERSION);

    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Requesting OAuth token for clientId:%s clientSecret:%s", clientId,
          clientSecret));
      }
      oAuthToken = WebtrendsSAPIAuth.getToken(clientId, clientSecret);
      if (LOG.isInfoEnabled()) {
        LOG.info("OAuth token aquired: " + oAuthToken);
      }
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException("Error requesting OAuth token", e);
    }
  }

  /**
   * Start processing Webtrends Streams API events
   */
  @Override
  public synchronized void start() {
    client = new WebSocketClient();
    SAPIWebSocket socket = new SAPIWebSocket();
    try {
      client.start();
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      if (LOG.isInfoEnabled()) {
        LOG.info("Connecting to web socket: " + streamURI);
      }
      client.connect(socket, new URI(streamURI), request);
      socket.await(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Unable to connect to web socket", e);
    }
    super.start();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void stop() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Closing web socket");
    }
    try {
      client.stop();
    } catch (Exception e) {
      LOG.error("Unable to close web socket", e);
    }
    super.stop();
  }

  /**
   * Web socket event handler
   */
  @WebSocket
  private class SAPIWebSocket {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final ChannelProcessor channel = getChannelProcessor();
    private final Map<String, String> headers = new HashMap<String, String>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
      // build SAPI query object
      final StringBuilder sb = new StringBuilder();
      sb.append("{\"access_token\":\"");
      sb.append(oAuthToken);
      sb.append("\",\"command\":\"stream\"");
      sb.append(",\"stream_type\":\"");
      sb.append(streamType);
      sb.append("\",\"query\":\"");
      sb.append(streamQuery);
      sb.append("\",\"api_version\":\"");
      sb.append(streamVersion);
      sb.append("\",\"schema_version\":\"");
      sb.append(streamSchemaVersion);
      sb.append("\"}");

      if (LOG.isInfoEnabled()) {
        LOG.info("Opening stream: " + sb.toString());
      }
      try {
        session.getRemote().sendString(sb.toString());
      } catch (IOException e) {
        throw new RuntimeException("Unable to open stream", e);
      }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
      headers.put(TimestampInterceptor.Constants.TIMESTAMP,
        Long.toString(System.currentTimeMillis()));
      Event event = EventBuilder.withBody(message.getBytes(), headers);
      channel.processEvent(event);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Web socket closed: " + statusCode);
      }
    }

    public boolean await(int duration, TimeUnit unit) throws InterruptedException {
      return latch.await(duration, unit);
    }
  }
}