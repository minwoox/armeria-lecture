package armeria.lecture.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.server.Server;

public class ReactiveServer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveServer.class);

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8080)
                                    .requestTimeoutMillis(0)
                                    .service("/animation", new AnimationService(300))
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));
    }

}
