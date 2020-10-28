package armeria.lecture.week5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.SimpleDecoratingHttpClient;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

class FilteredStreamMessageTest {

    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", (ctx, req) -> {
                final HttpResponseWriter streaming = HttpResponse.streaming();
                streaming.write(ResponseHeaders.of(200));
                streaming.write(HttpData.ofUtf8("Hello "));
                streaming.write(HttpData.ofUtf8("world"));
                streaming.write(HttpHeaders.of("total-length", 11));
                streaming.close();
                return streaming;
            });
        }
    };

    @Test
    void client() {
        final WebClient client = WebClient.builder(server.httpUri())
                                          .decorator(LoggingClient.builder()
                                                                  .requestLogLevel(LogLevel.INFO)
                                                                  .successfulResponseLogLevel(LogLevel.INFO)
                                                                  .newDecorator())
                                          .decorator(FilteringHttpClient::new)
                                          .build();

        System.err.println(client.get("/").aggregate().join().contentUtf8());
    }

    private static class FilteringHttpClient extends SimpleDecoratingHttpClient {

        protected FilteringHttpClient(HttpClient delegate) {
            super(delegate);
        }

        @Override
        public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
            return null;
        }
    }
}
