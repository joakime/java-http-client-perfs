package perfs;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.HdrHistogram.Histogram;
import org.eclipse.jetty.toolchain.perf.HistogramSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        URI destUri = URI.create("http://ceres:9797/test/dump/info");
        Main main = new Main();

        List<IHttpClient> clientImpls = List.of(
            new JdkClient(),
            new ApacheDefaultBlockingClient(),
            new ApacheInternalBlockingClient(),
            new ApacheMinimalBlockingClient(),
            new JettyBlockingClient()
        );

        for (IHttpClient client : clientImpls)
        {
            main.test(client, destUri);
        }

        System.exit(0);
    }

    private void test(IHttpClient clientImpl, URI destUri)
    {
        try
        {
            startClient(clientImpl);

            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(200);
            AtomicInteger num = new AtomicInteger(0);
            Histogram histogram = new Histogram(3);

            LOG.info("Using POST {} to {}", destUri, clientImpl);
            long nanoStart = System.nanoTime();

            try
            {
                ScheduledFuture<?> fixedrate = scheduledExecutorService.scheduleAtFixedRate(() ->
                {
                    run(clientImpl, histogram, destUri, num);
                }, 0, 10, TimeUnit.MILLISECONDS);
                scheduledExecutorService.schedule(() ->
                {
                    LOG.info("Cancelling fixed rate");
                    fixedrate.cancel(false);
                    scheduledExecutorService.shutdown();
                }, 10, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                LOG.warn("Unable to issue POST request to {}", destUri);
            }

            try
            {
                scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            long nanoEnd = System.nanoTime();

            LOG.info(String.format("Done: runs [%d] using %s took %,d ns", num.get(), clientImpl, nanoEnd - nanoStart));
            System.err.println(new HistogramSnapshot(histogram, 20, "Messages - Latency", "\u00B5s", TimeUnit.NANOSECONDS::toMicros));
        }
        finally
        {
            stopClient(clientImpl);
        }
    }

    private void stopClient(IHttpClient clientImpl)
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

    private void startClient(IHttpClient clientImpl)
    {
        try
        {
            clientImpl.start();
        }
        catch (Exception e)
        {
            LOG.warn("Unable to start {}", clientImpl, e);
        }
    }

    private void run(IHttpClient clientImpl, Histogram histogram, URI destUri, AtomicInteger num)
    {
        URI uri = URI.create(destUri.toASCIIString() + "?num=" + num.incrementAndGet());
        try
        {
            // LOG.info("Run POST to {}", uri);
            long startNanos = System.nanoTime();
            String responseBody = clientImpl.post(uri, "text/plain",
                "This is request body number [" + num.get() + "]");
            long endNanos = System.nanoTime();
            long latency = endNanos - startNanos;
            histogram.recordValue(latency);
            // LOG.info("Got {} bytes in response", responseBody.length());
        }
        catch (Exception e)
        {
            LOG.warn("POST to {}", uri, e);
        }
    }
}
