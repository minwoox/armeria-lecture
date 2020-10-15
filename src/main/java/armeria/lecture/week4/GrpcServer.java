package armeria.lecture.week4;

import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.grpc.GrpcService;

public final class GrpcServer {

    public static void main(String[] args) {
        final GrpcService grpcService =
                GrpcService.builder()
                           .addService(new HelloService())
                           .enableUnframedRequests(true)
                           .build();

        final Server server = Server.builder()
                                    .http(8088)
                                    .service(grpcService)
                                    .serviceUnder("/docs/", DocService.builder().build())
                                    .build();
        server.start().join();
    }
}
