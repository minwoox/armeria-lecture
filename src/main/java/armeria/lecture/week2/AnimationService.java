package armeria.lecture.week2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;

import io.netty.channel.EventLoop;

public final class AnimationService implements HttpService {

    private static final List<String> frames = Arrays.asList(
            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    │││ \\   ║\n" +
            "║    │││  O  ║\n" +
            "║    OOO     ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║   / │││    ║\n" +
            "║  O  │││    ║\n" +
            "║     OOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>"
    );
    private final long frameIntervalMillis;

    public AnimationService(long frameIntervalMillis) {
        this.frameIntervalMillis = frameIntervalMillis;
    }

    @Override
    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        final HttpResponseWriter res = HttpResponse.streaming();
        res.write(ResponseHeaders.of(HttpStatus.OK, HttpHeaderNames.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8));
        res.whenConsumed().thenRun(() -> streamData(ctx.eventLoop(), res, 0));
        return res;
    }

    private void streamData(EventLoop eventLoop, HttpResponseWriter res, int frameIndex) {
        res.write(HttpData.ofUtf8(frames.get(frameIndex)));
        res.whenConsumed().thenRun(() -> eventLoop.schedule(
                () -> streamData(eventLoop, res, (frameIndex + 1) % frames.size()),
                frameIntervalMillis, TimeUnit.MILLISECONDS));
    }
}
