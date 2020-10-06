package armeria.lecture.week2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import armeria.lecture.week2.ServiceInfoServer.DiscoveryService;
import armeria.lecture.week2.ServiceInfoServer.RegistrationService;

public class ServiceInfoServerTest {

    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            final ConcurrentLinkedQueue<String> addresses = new ConcurrentLinkedQueue<>();
            sb.route()
              .post("/registration")
              .build(new RegistrationService(addresses))
              .service("/discovery", new DiscoveryService(addresses));
        }
    };

    @Test
    void discovery() {
        final WebClient webClient = WebClient.of(server.httpUri());
        webClient.post("/registration", "127.0.0.1:10000").aggregate().join();
        webClient.post("/registration", "127.0.0.1:10001").aggregate().join();
        final AggregatedHttpResponse res = webClient.get("/discovery").aggregate().join();
        assertThat(res.contentUtf8()).isEqualTo("127.0.0.1:10000,127.0.0.1:10001");
    }
}
