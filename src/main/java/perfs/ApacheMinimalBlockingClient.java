package perfs;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.jetty.util.IO;

public class ApacheMinimalBlockingClient implements IHttpClient
{
    private CloseableHttpClient client;

    @Override
    public void start() throws Exception
    {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(25);
        connectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
        client = HttpClients.createMinimal(connectionManager);
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

        try (InputStream in = response.getEntity().getContent())
        {
            return IO.toString(in, StandardCharsets.UTF_8);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", this.getClass().getSimpleName(), client);
    }
}
