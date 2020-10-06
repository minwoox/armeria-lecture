package armeria.lecture.week2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

public class ServiceInfoServer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveServer.class);

    public static void main(String[] args) {
        final ConcurrentLinkedQueue<String> addresses = new ConcurrentLinkedQueue<>();
        final Server server = Server.builder()
                                    .http(9000)
                                    .route().post("/registration").build(new RegistrationService(addresses))
                                    .service("/discovery", new DiscoveryService(addresses))
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));
    }

    static class RegistrationService implements HttpService {

        private final ConcurrentLinkedQueue<String> addresses;

        RegistrationService(ConcurrentLinkedQueue<String> addresses) {
            this.addresses = addresses;
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            final CompletableFuture<HttpResponse> future =
                    new CompletableFuture<>();
            req.aggregate().thenAccept(aggregatedReq -> {
                addresses.add(aggregatedReq.contentUtf8());
                future.complete(HttpResponse.of(200));
            });
            return HttpResponse.from(future);
        }
    }

    static class DiscoveryService implements HttpService {

        private static final Joiner joiner = Joiner.on(',');

        private final ConcurrentLinkedQueue<String> addresses;

        DiscoveryService(ConcurrentLinkedQueue<String> addresses) {
            this.addresses = addresses;
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            final String addresses = joiner.join(this.addresses);
            return HttpResponse.of(addresses);
        }
    }
}
