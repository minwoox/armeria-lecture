package armeria.lecture.week5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreaker;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerClient;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerRule;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

class CircuitBreakerTest {
    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", (ctx, req) -> HttpResponse.of(503));
        }
    };

    @Test
    void circuit() throws InterruptedException {
        final WebClient client = WebClient.builder(server.httpUri())
                                          .decorator(CircuitBreakerClient.newPerHostDecorator(
                                                  CircuitBreaker::of, CircuitBreakerRule.onServerErrorStatus()))
                                          .build();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            Thread.sleep(200);
            try {
                client.get("/").aggregate().join();
            } catch (Exception e) {
                System.err.println(e);
                System.err.println("index: " + i);
                return;
            }
        }

    }
}
