package perfs;

import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        URI destUri = URI.create("http://ceres:9797/test/dump/info");

        List<IHttpClient> clientImpls = List.of(
            new JettyBlockingClient(),
            new ApacheBlockingClient(),
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

                    long nanoStart = System.nanoTime();
                    int runs = 10;
                    int iterations = 128;
                    IntStream.range(0, 16).parallel().forEach(i ->
                        IntStream.range(0, runs).forEach(j ->
                        {
                            run(clientImpl, destUri, (i + j), iterations);
                        })
                    );
                    long nanoEnd = System.nanoTime();
                    System.out.printf("Done: runs [%d] iterations [%d] using %s took %,d ns%n", runs, iterations, clientImpl, nanoEnd - nanoStart);
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

    private static void run(IHttpClient clientImpl, URI destUri, int num, int iterations)
    {
        try
        {
            for (int i = 0; i < iterations; i++)
            {
                URI uri = URI.create(destUri.toASCIIString() + "?num=" + num + "&iter=" + i + "&iterMax=" + iterations);
                String responseBody = clientImpl.post(uri, "text/plain",
                    "This is request body number [" + num + "]");
                // LOG.info("Got {} bytes in response", responseBody.length());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
