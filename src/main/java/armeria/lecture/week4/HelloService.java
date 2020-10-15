package armeria.lecture.week4;

import com.example.grpc.hello.HelloReply;
import com.example.grpc.hello.HelloRequest;
import com.example.grpc.hello.HelloServiceGrpc.HelloServiceImplBase;

import io.grpc.stub.StreamObserver;

public final class HelloService extends HelloServiceImplBase {
    @Override
    public void hello(HelloRequest request,
                      StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder()
                                          .setMessage("Hello " + request.getName())
                                          .build());
        responseObserver.onCompleted();
    }

    @Override
    public void helloStream(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder()
                                          .setMessage("Hello " + request.getName())
                                          .build());
        responseObserver.onNext(HelloReply.newBuilder()
                                          .setMessage("Hello " + request.getName() + ", again")
                                          .build());
        responseObserver.onCompleted();
    }
}
