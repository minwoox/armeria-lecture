package armeria.lecture.week5;

import static armeria.lecture.week5.AttributeTest.startBackend;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.ClientRequestContextCaptor;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.util.SafeCloseable;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

class PooledObjectTest {

    @Test
    void pooledObject() {
        startBackend(8081);
        Server.builder()
              .http(8080)
              .service("/", new SimpleService())
              .build().start().join();

        final WebClient client = WebClient.of("http://127.0.0.1:8080");
        try (ClientRequestContextCaptor captor = Clients.newContextCaptor()) {
            final HttpResponse response = client.get("/");
            final ClientRequestContext ctx = captor.get();
            final CompletableFuture<AggregatedHttpResponse> aggregated =
                    response.aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc());
            final AggregatedHttpResponse httpResponse = aggregated.join();
            final ByteBuf byteBuf;
            try (HttpData content = httpResponse.content()) {
                System.err.println(content.toStringUtf8());
                byteBuf = content.byteBuf();
                System.err.println(content.byteBuf().refCnt());
                System.err.println(httpResponse.contentUtf8());
            }
            System.err.println(byteBuf.refCnt());
        }
    }

    private static class SimpleService implements HttpService {

        private final WebClient backendClient;

        SimpleService() {
            backendClient = WebClient.of("http://127.0.0.1:8081");
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
            final CompletableFuture<AggregatedHttpResponse> aggregated =
                    backendClient.execute(req)
                                 .aggregateWithPooledObjects(ctx.eventLoop(), ctx.alloc());

            aggregated.thenAcceptAsync(aggregatedRes -> {
                final HttpData content = aggregatedRes.content();
                future.complete(HttpResponse.of(aggregatedRes.headers(), content));
            }, ctx.eventLoop());
            return HttpResponse.from(future);
        }
    }
}
