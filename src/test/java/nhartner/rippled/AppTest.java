package nhartner.rippled;

import static org.junit.Assert.assertThat;

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
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.io.IOException;

/**
 * Unit test for simple App.
 */
public class AppTest {

    public static GenericContainer rippledContainer =
      new GenericContainer("xrptipbot/rippled:latest")
        .withCommand("-a")
        .withExposedPorts(5005)
        .withClasspathResourceMapping("rippled",
          "/config",
          BindMode.READ_ONLY)
        .waitingFor(new HostPortWaitStrategy());

    static {
        rippledContainer.start();
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
