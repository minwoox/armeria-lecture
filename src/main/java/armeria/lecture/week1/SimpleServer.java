package armeria.lecture.week1;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;

public class SimpleServer {

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8088)
                                    .service("/hello", (ctx, req) -> HttpResponse.of(200))
                                    .build();
        server.start().join();
        final WebClient client = WebClient.builder("http://127.0.0.1:8088/")
                                          .build();
        final AggregatedHttpResponse res = client.get("/hello").aggregate().join();
        System.err.println(res.headers());
        server.stop().join();
    }
}
