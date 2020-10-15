package armeria.lecture.week4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.example.grpc.hello.HelloReply;
import com.example.grpc.hello.HelloRequest;
import com.example.grpc.hello.HelloServiceGrpc.HelloServiceBlockingStub;
import com.example.grpc.hello.HelloServiceGrpc.HelloServiceFutureStub;
import com.example.grpc.hello.HelloServiceGrpc.HelloServiceStub;
import com.google.common.util.concurrent.ListenableFuture;

import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import io.grpc.stub.StreamObserver;

class GrpcServiceTest {

    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            final GrpcService service = GrpcService.builder()
                                                   .addService(new HelloService())
                                                   .useBlockingTaskExecutor(true)
                                                   .build();
            sb.service(service);

        }
    };

    @Test
    void blockingStub() {
        final HelloServiceBlockingStub helloServiceBlockingStub = Clients.newClient(
                server.httpUri(GrpcSerializationFormats.PROTO), HelloServiceBlockingStub.class);
        final HelloReply reply = helloServiceBlockingStub.hello(
                HelloRequest.newBuilder().setName("Armeria!").build());
        assertThat(reply.getMessage()).isEqualTo("Hello Armeria!");
    }

    @Test
    void stub() throws InterruptedException {
        final HelloServiceStub stub = Clients.builder(server.httpUri(GrpcSerializationFormats.PROTO)).build(
                HelloServiceStub.class);
        final CountDownLatch latch = new CountDownLatch(2);
        stub.hello(HelloRequest.newBuilder().setName("Armeria!").build(), new StreamObserver<>() {
            @Override
            public void onNext(HelloReply value) {
                assertThat(value.getMessage()).isEqualTo("Hello Armeria!");
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    void futureStub() throws Exception {
        final HelloServiceFutureStub futureStub =
                Clients.builder(server.httpUri(GrpcSerializationFormats.PROTO))
                       .build(HelloServiceFutureStub.class);
        final ListenableFuture<HelloReply> future = futureStub.hello(
                HelloRequest.newBuilder().setName("Armeria!").build());
        final HelloReply reply = future.get();
        assertThat(reply.getMessage()).isEqualTo("Hello Armeria!");
    }

    @Test
    void helloStream() throws InterruptedException {
        final HelloServiceStub stub = Clients.builder(server.httpUri(GrpcSerializationFormats.PROTO)).build(
                HelloServiceStub.class);
        final CountDownLatch latch = new CountDownLatch(1);
        stub.helloStream(HelloRequest.newBuilder().setName("Armeria!").build(), new StreamObserver<>() {
            @Override
            public void onNext(HelloReply value) {
                System.err.println(value.getMessage());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });
        latch.await();
    }
}
