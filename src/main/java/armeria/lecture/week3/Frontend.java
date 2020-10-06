package armeria.lecture.week3;

import java.util.concurrent.CompletableFuture;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.brave.BraveClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.util.SafeCloseable;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.brave.BraveService;
import com.linecorp.armeria.server.logging.LoggingService;

import brave.Tracing;

public final class Frontend {

    public static void main(String[] args) {
        final Tracing tracing = TracingFactory.create("frontend");

        final WebClient backendClient =
                WebClient.builder("http://localhost:9000/")
                         .decorator(BraveClient.newDecorator(tracing, "backend"))
                         .build();
        final WebClient backend1Client =
                WebClient.builder("http://localhost:9001/")
                         .decorator(BraveClient.newDecorator(tracing, "backend1"))
                         .build();

        final Server server =
                Server.builder()
                      .http(8081)
                      .service("/", (ctx, req) -> {
                          final CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                          final CompletableFuture<AggregatedHttpResponse> aggregated =
                                  backendClient.get("/api").aggregate();
                          aggregated.thenAccept(aggregatedHttpResponse -> {
                              try (SafeCloseable ignored = ctx.push()) {
                                  final HttpResponse response = backend1Client.get("/api");
                                  future.complete(response);
                              }
                          });
                          return HttpResponse.from(future);
                      })
                      .decorator(BraveService.newDecorator(tracing))
                      .decorator(LoggingService.newDecorator())
                      .build();

        server.start().join();
    }
}
