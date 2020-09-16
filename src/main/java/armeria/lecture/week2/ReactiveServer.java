package armeria.lecture.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.common.ServerCacheControl;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.file.HttpFile;

public class ReactiveServer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveServer.class);

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8080)
                                    .requestTimeoutMillis(0)
                                    .service("/html/", HttpFile.builder(ReactiveServer.class.getClassLoader(),
                                                                       "index.html")
                                                              .cacheControl(ServerCacheControl.REVALIDATED)
                                                              .build().asService())
                                    .service("/animation", new AnimationService(300))
                                    .build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));
    }

}
