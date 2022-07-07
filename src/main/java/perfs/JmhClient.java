package perfs;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class JmhClient
{
    @Param({"JDK", "JETTYBLOCKING", "APACHEBLOCKING"})
    public static String clientClass;

    private IHttpClient client;

    private URI destUri = URI.create("http://ceres:9797/test/dump/info");

    private AtomicInteger count;

    @Setup
    public void setup() throws Exception
    {
        count = new AtomicInteger(0);

        switch (clientClass)
        {
            case "JDK":
                client = new JdkClient();
                break;
            case "JETTYBLOCKING":
                client = new JettyBlockingClient();
                break;
            case "APACHEBLOCKING":
                client = new ApacheInternalBlockingClient();
                break;
        }
        client.start();
    }

    @TearDown
    public void teardown() throws Exception
    {
        client.stop();
        System.out.println("counter = " + count.get());
    }

    @Benchmark
    @Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
    @Threads(20)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String testRequest() throws Exception
    {
        return client.post(destUri, "text/plain",
            "This is request body number [" + count.incrementAndGet() + "]");
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
            .include(JmhClient.class.getSimpleName())
            .forks(1)
            .addProfiler(AsyncProfiler.class, "output=flamegraph")
            .build();

        new Runner(opt).run();
    }
}
