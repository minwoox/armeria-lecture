package armeria.lecture.week2;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerListener;

public class MyUpdatingListener implements ServerListener {

    private final int port;
    private WebClient client;

    MyUpdatingListener(int port) {
        this.port = port;
        client = WebClient.of("http://127.0.0.1:9000");
    }

    @Override
    public void serverStarting(Server server) throws Exception {

    }

    @Override
    public void serverStarted(Server server) throws Exception {
        client.post("/registration", "127.0.0.1:" + port).aggregate().thenApply(aggregatedRes -> {
            System.err.println(aggregatedRes.headers().status());
            return null;
        });
    }

    @Override
    public void serverStopping(Server server) throws Exception {
        // client.delete("/deregistration").aggregate();
    }

    @Override
    public void serverStopped(Server server) throws Exception {

    }
}
