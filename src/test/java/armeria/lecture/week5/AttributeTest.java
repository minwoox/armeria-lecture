package armeria.lecture.week5;

import org.junit.jupiter.api.Test;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;

class AttributeTest {

    @Test
    void attribute() {
        startBackend(8081);
        Server.builder()
              .http(8080)
              .service("/", new SimpleService())
              .decorator(RequestIdService::new)
              .build().start().join();

        final WebClient client = WebClient.of("http://127.0.0.1:8080");
        System.err.println(client.get("/").aggregate().join().contentUtf8());
    }

    static void startBackend(int port) {
        final Server server =
                Server.builder()
                      .http(port)
                      .service("/", (ctx, req) -> HttpResponse.of("Hello world"))
                      .build();
        server.start().join();
    }

    private static class SimpleService implements HttpService {

        private final WebClient backendClient;

        SimpleService() {
            backendClient = WebClient.builder("http://127.0.0.1:8081")
                                     .build();
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return null;
        }

        private static void printRequestId(String method, RequestContext ctx) {
            System.err.println("requestId in " + method + ": ");
        }
    }

    private static class RequestIdService extends SimpleDecoratingHttpService {

        protected RequestIdService(HttpService delegate) {
            super(delegate);
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return null;
        }
    }
}
