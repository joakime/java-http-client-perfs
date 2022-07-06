package perfs;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        URI destUri = URI.create("http://ceres:9797/test/dump/info");

        List<IHttpClient> clientImpls = List.of(
            new JettyClient(),
            new ApacheClient(),
            new JdkClient()
        );

        for (IHttpClient clientImpl : clientImpls)
        {
            try
            {
                try
                {
                    clientImpl.start();
                }
                catch (Exception e)
                {
                    LOG.warn("Unable to start {}", clientImpl, e);
                }

                try
                {
                    LOG.info("Issuing POST {} via {}", destUri, clientImpl);
                    String responseBody = clientImpl.post(destUri, "text/plain", "This is the Request Body");
                    LOG.info("Got {} bytes in response", responseBody.length());
                }
                catch (Exception e)
                {
                    LOG.warn("Unable to issue POST request to {}", destUri);
                }
            }
            finally
            {
                try
                {
                    clientImpl.stop();
                }
                catch (Exception e)
                {
                    LOG.warn("Unable to stop {}", clientImpl, e);
                }
            }
        }
    }
}
