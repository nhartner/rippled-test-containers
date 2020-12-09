package nhartner.rippled;

import static org.junit.Assert.assertThat;

import com.github.dockerjava.api.command.CreateContainerCmd;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private static final OkHttpClient CLIENT = new OkHttpClient();


    public static GenericContainer rippledContainer =
      new GenericContainer("xrptipbot/rippled:latest")
        .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) (cmd) ->
          cmd.withEntrypoint("/opt/ripple/bin/rippled"))
        // run in standalone mode with fresh ledger and all amendments enabled
        .withCommand("-a --start --conf /config/rippled.cfg")
        .withExposedPorts(5005)
        .withClasspathResourceMapping("rippled",
          "/config",
          BindMode.READ_ONLY)
        .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Application starting.*"));

    private static final Runnable LEDGER_ACCEPTOR = () -> {
        Request request = new Request.Builder()
          .url("http://" + rippledContainer.getHost() + ":" + rippledContainer.getMappedPort(5005) + "/")
          .method("POST", RequestBody.create("{ \"method\": \"ledger_accept\" }", MediaType.parse("application/json")))
          .build();

        Call call = CLIENT.newCall(request);
        try {
            call.execute();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    };

    static {
        rippledContainer.start();
        // advance ledger every 3 seconds to mimic the behavior of a real XRP ledger network.
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(LEDGER_ACCEPTOR, 0, 3, TimeUnit.SECONDS);
    }

    @Test
    public void advanceLedger() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
          .url("http://" + rippledContainer.getHost() + ":" + rippledContainer.getMappedPort(5005) + "/")
          .method("POST", RequestBody.create("{ \"method\": \"ledger_accept\" }", MediaType.parse("application/json")))
          .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        assertThat(response.body().string(), CoreMatchers.containsString("\"status\":\"success\""));
    }

}
