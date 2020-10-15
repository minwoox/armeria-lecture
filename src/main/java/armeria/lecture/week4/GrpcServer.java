package armeria.lecture.week4;

import com.linecorp.armeria.server.Server;

public final class GrpcServer {

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8088)
                                    .build();
        server.start().join();
    }
}
