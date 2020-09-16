package armeria.lecture.week1;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.ResponseHeaders;

import armeria.lecture.week1.ReactiveStreamsSubscriberTest.MyAggregatedHttpResponse;

class ReactiveStreamsPublisherTest {

    @Test
    void streaming() {
        final HttpResponseWriter res = HttpResponse.streaming();
        res.write(ResponseHeaders.of(200));
        res.write(HttpData.ofUtf8("foo"));
        res.close();
        assert res instanceof Publisher;

        final CompletableFuture<AggregatedHttpResponse> aggregated = res.aggregate();
        final AggregatedHttpResponse aggregatedHttpResponse = aggregated.join();
        System.err.println(aggregatedHttpResponse.headers().status());
        System.err.println(aggregatedHttpResponse.contentUtf8());
    }

    @Test
    void customPublisher() {
//        final MyHttpResponse res = new MyHttpResponse();
//        final CompletableFuture<MyAggregatedHttpResponse> aggregated = aggregate(res);
//        res.write(ResponseHeaders.of(200));
//        res.write(HttpData.ofUtf8("foo"));
//        res.close();
//
//        final MyAggregatedHttpResponse aggregatedHttpResponse = aggregated.join();
//        System.err.println(aggregatedHttpResponse.headers().status());
//        System.err.println(aggregatedHttpResponse.contentUtf8());
    }

    // We need a thread to use such as we used a thread from the fork join pool.
    // Use ConcurrentLinkedQueue to store data.
    // Don't have to implement cancel.
}
