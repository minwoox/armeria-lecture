package armeria.lecture.week5;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.DecoratingHttpClientFunction;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.logging.RequestScopedMdc;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;

import io.netty.util.AttributeKey;

class AttributeTest {

    private static final Logger logger = LoggerFactory.getLogger(AttributeTest.class);

    private static final AttributeKey<Long> requestIdKey =
            AttributeKey.valueOf(AttributeTest.class, "requestIdKey");

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
                                     .decorator(new DecoratingHttpClientFunction() {
                                         @Override
                                         public HttpResponse execute(HttpClient delegate,
                                                                     ClientRequestContext ctx, HttpRequest req)
                                                 throws Exception {
                                             printRequestId("client", ctx);
                                             return delegate.execute(ctx, req);
                                         }
                                     })
                                     .build();
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
            logger.info("Send a request to backend");
            final CompletableFuture<AggregatedHttpResponse> aggregated =
                    backendClient.execute(req).aggregate();

            printRequestId("service", ctx);
            aggregated.thenAcceptAsync(aggregatedRes -> {
                future.complete(aggregatedRes.toHttpResponse());
                logger.info("Send a response back to client");
            }, ctx.eventLoop());
            return HttpResponse.from(future);
        }

        private static void printRequestId(String method, RequestContext ctx) {
            System.err.println("requestId in " + method + ": " + ctx.ownAttr(requestIdKey));
        }
    }

    private static class RequestIdService extends SimpleDecoratingHttpService {

        protected RequestIdService(HttpService delegate) {
            super(delegate);
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            final long requestId = ThreadLocalRandom.current().nextLong();
            ctx.setAttr(requestIdKey, requestId);
            MDC.put("requestId", String.valueOf(requestId));
            RequestScopedMdc.put(ctx, "requestId", String.valueOf(requestId));
            final HttpResponse response = unwrap().serve(ctx, req);
            MDC.remove("requestId");
            return response;
        }
    }
}
