package perfs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JdkClient implements IHttpClient
{
    private HttpClient client;

    @Override
    public void start() throws Exception
    {
        client = HttpClient.newHttpClient();
    }

    @Override
    public void stop() throws Exception
    {
        // nothing to close/stop
    }

    @Override
    public String post(URI uri, String requestContentType, String requestBody) throws Exception
    {
        HttpRequest builder = HttpRequest
            .newBuilder(uri)
            .header("Content-Type", requestContentType)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        HttpResponse<String> response = client.send(builder, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Bad response status: " + response.statusCode());
        return response.body();
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName();
    }
}
