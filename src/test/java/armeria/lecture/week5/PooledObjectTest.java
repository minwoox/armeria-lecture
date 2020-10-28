package armeria.lecture.week5;

import static armeria.lecture.week5.AttributeTest.startBackend;

import org.junit.jupiter.api.Test;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

class PooledObjectTest {

    @Test
    void pooledObject() {
        startBackend(8081);
        Server.builder()
              .http(8080)
              .service("/", new SimpleService())
              .build().start().join();

        final WebClient client = WebClient.of("http://127.0.0.1:8080");
        System.err.println(client.get("/").aggregate().join().contentUtf8());
    }

    private static class SimpleService implements HttpService {

        private final WebClient backendClient;

        SimpleService() {
            backendClient = WebClient.of("http://127.0.0.1:8081");
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return null;
        }
    }
}
