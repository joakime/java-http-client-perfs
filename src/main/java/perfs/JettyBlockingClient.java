package perfs;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.component.LifeCycle;

public class JettyBlockingClient implements IHttpClient
{
    private HttpClient client;

    @Override
    public void start() throws Exception
    {
        client = new HttpClient();
        client.setConnectTimeout(TimeUnit.SECONDS.toMillis(30));
        client.setFollowRedirects(false);
        client.setIdleTimeout(TimeUnit.SECONDS.toMillis(30));
        client.setName("jetty-client");
        client.setSocketAddressResolver(new SocketAddressResolver.Sync());
        client.start();
        client.getContentDecoderFactories().clear();
    }

    @Override
    public void stop()
    {
        LifeCycle.stop(client);
    }

    @Override
    public String post(URI uri, String requestContentType, String requestBody) throws Exception
    {
        Request request = client.POST(uri);
        StringRequestContent entity = new StringRequestContent(requestBody);
        request.headers((headers) -> headers.put(HttpHeader.CONTENT_TYPE, requestContentType));
        request.body(entity);
        ContentResponse response = request.send();
        if (response.getStatus() != 200)
            throw new RuntimeException("Bad response status: " + response.getStatus());
        return response.getContentAsString();
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName();
    }
}
