package perfs;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class ApacheInternalBlockingClient implements IHttpClient
{
    private CloseableHttpClient client;

    @Override
    public void start() throws Exception
    {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(25);
        connectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
        RequestConfig.Builder custom = RequestConfig.custom();
        custom.setConnectTimeout(10000);
        custom.setConnectionRequestTimeout(10000);
        custom.setSocketTimeout(10000);
        RequestConfig config = custom.build();

        client = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(config)
            .disableContentCompression()
            .disableAutomaticRetries()
            .evictExpiredConnections()
            .build();
    }

    @Override
    public void stop() throws Exception
    {
        client.close();
    }

    @Override
    public String post(URI uri, String contentType, String requestBody) throws Exception
    {
        HttpPost httpPost = new HttpPost(uri);
        StringEntity entity = new StringEntity(requestBody);
        entity.setContentType(contentType);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = client.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200)
            throw new RuntimeException("Bad response status: " + response.getStatusLine());

        return EntityUtils.toString(response.getEntity());
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", this.getClass().getSimpleName(), client);
    }
}
