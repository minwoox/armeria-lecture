package armeria.lecture.week5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.SimpleDecoratingHttpClient;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.encoding.DecodingClient;
import com.linecorp.armeria.client.logging.ContentPreviewingClient;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.common.FilteredHttpResponse;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaTypeNames;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.common.logging.RequestLogAccess;
import com.linecorp.armeria.common.logging.RequestLogProperty;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.encoding.EncodingService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

class FilteredStreamMessageTest {

    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service("/", (ctx, req) -> {
                final HttpResponseWriter streaming = HttpResponse.streaming();
                streaming.write(ResponseHeaders.of(
                        HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaTypeNames.PLAIN_TEXT_UTF_8));
                streaming.write(HttpData.ofUtf8("Hello "));
                streaming.write(HttpData.ofUtf8("world"));
                streaming.write(HttpHeaders.of("total-length", 11));
                streaming.close();
                return streaming;
            });
            sb.decorator(EncodingService.newDecorator());
        }
    };

    @Test
    void client() {
        final WebClient client = WebClient.builder(server.httpUri())
                                          .decorator(DecodingClient.newDecorator())
                                          .decorator(ContentPreviewingClient.newDecorator(100))
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
            final RequestLogAccess log = ctx.log();
            log.whenAvailable(RequestLogProperty.RESPONSE_END_TIME).thenAccept(rLog -> {
               System.err.println("response end");
            });
            log.whenAvailable(RequestLogProperty.RESPONSE_HEADERS).thenAccept(rLog -> {
                System.err.println("response headers");
            });
            log.whenAvailable(RequestLogProperty.REQUEST_END_TIME).thenAccept(rLog -> {
                System.err.println("request end");
            });
            log.whenAvailable(RequestLogProperty.REQUEST_HEADERS).thenAccept(rLog -> {
                System.err.println("request headers");
            });
            final HttpResponse response = unwrap().execute(ctx, req);
            final FilteredHttpResponse filtered = new FilteredHttpResponse(response) {
                @Override
                protected HttpObject filter(HttpObject obj) {
                    if (obj instanceof ResponseHeaders) {
                        //
                    } else if (obj instanceof HttpData) {
                        System.err.println(((HttpData) obj).toStringUtf8());
                    } else {
                        assert obj instanceof HttpHeaders; // trailers
                    }
                    return obj;
                }
            };

            return filtered;
        }
    }
}
