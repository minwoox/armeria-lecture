package armeria.lecture.week1;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.ResponseHeaders;

import armeria.lecture.week1.ReactiveStreamsSubscriberTest.MyAggregatedHttpResponse;
import io.netty.channel.EventLoop;

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
        final MyHttpResponse res = new MyHttpResponse();
        final CompletableFuture<MyAggregatedHttpResponse> aggregated = aggregate(res);
        res.write(ResponseHeaders.of(200));
        res.write(HttpData.ofUtf8("foo"));
        res.close();

        final MyAggregatedHttpResponse aggregatedHttpResponse = aggregated.join();
        System.err.println(aggregatedHttpResponse.headers().status());
        System.err.println(aggregatedHttpResponse.contentUtf8());
    }

    // We need a thread to use such as we used a thread from the fork join pool.
    static EventLoop defaultSubscriberExecutor() {
        return CommonPools.workerGroup().next();
    }

    // Use ConcurrentLinkedQueue to store data.
    // Don't have to implement cancel.

    private static class MyHttpResponse implements Publisher<HttpObject> {

        private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
        @Nullable
        private volatile MySubscription subscription;

        public void write(HttpObject obj) {
            queue.add(obj);
            notifySubscriber();
        }

        private void notifySubscriber() {
            final MySubscription subscription = this.subscription;
            if (subscription == null) {
                return;
            }
            subscription.executor().execute(subscription::notifySubscriber);
        }

        public void close() {
            queue.add(new CloseEvent());
        }

        @Override
        public void subscribe(Subscriber<? super HttpObject> s) {
            final EventLoop executor = defaultSubscriberExecutor();
            final MySubscription subscription = new MySubscription(executor, s);
            this.subscription = subscription;
            executor.execute(() -> s.onSubscribe(subscription));
        }

        private class MySubscription implements Subscription {

            private final EventLoop executor;
            private final Subscriber<? super HttpObject> subscriber;
            private long demand;

            MySubscription(EventLoop executor, Subscriber<? super HttpObject> subscriber) {
                this.executor = executor;
                this.subscriber = subscriber;
            }

            @Override
            public void request(long n) {
                demand += n;
                notifySubscriber();
            }

            private void notifySubscriber() {
                while (demand > 0) {
                    final Object obj = queue.poll();
                    if (obj == null) {
                        return;
                    }
                    if (obj instanceof HttpObject) {
                        demand--;
                        subscriber.onNext((HttpObject) obj);
                    } else {
                        assert obj instanceof CloseEvent;
                        subscriber.onComplete();
                    }
                }
            }

            @Override
            public void cancel() {}

            EventLoop executor() {
                return executor;
            }
        }

        private static class CloseEvent {}
    }

    private CompletableFuture<MyAggregatedHttpResponse> aggregate(MyHttpResponse res) {
        final CompletableFuture<MyAggregatedHttpResponse> result = new CompletableFuture<>();
        res.subscribe(new Subscriber<>() {
            private HttpData httpData;
            private ResponseHeaders responseHeaders;
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(HttpObject httpObject) {
                if (httpObject instanceof ResponseHeaders) {
                    responseHeaders = (ResponseHeaders) httpObject;
                } else {
                    assert httpObject instanceof HttpData;
                    httpData = (HttpData) httpObject;
                }
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {
                result.complete(new MyAggregatedHttpResponse(responseHeaders, httpData));
            }
        });
        return result;
    }
}
