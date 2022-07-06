package perfs;

import java.net.URI;

public interface IHttpClient
{
    void start() throws Exception;

    void stop() throws Exception;

    String post(URI uri, String requestContentType, String requestBody) throws Exception;
}
