package armeria.lecture.week2;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

public class ProxyServer {

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8000)
                                    .requestTimeoutMillis(0)
                                    .serviceUnder("/", new ProxyService())
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            System.err.println("Server has been stopped.");
        }));
    }

    private static class ProxyService implements HttpService {

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return null;
        }
    }
}
