package armeria.lecture.week2;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.endpoint.EndpointGroup;
import com.linecorp.armeria.client.endpoint.healthcheck.HealthCheckedEndpointGroup;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

public class ProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveServer.class);

    public static void main(String[] args) {
        final Server server = Server.builder()
                                    .http(8080)
                                    .requestTimeoutMillis(0)
                                    .serviceUnder("/", new ProxyService()).build();
        server.start().join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop().join();
            logger.info("Server has been stopped.");
        }));
    }

    private static class ProxyService implements HttpService {

        private static final Splitter csvSplitter = Splitter.on(',').trimResults();
        private static final Splitter colonSplitter = Splitter.on(':').trimResults();
        private final WebClient client;

        ProxyService() {
            final WebClient discoveryClient = WebClient.of("http://127.0.0.1:9000");
            final AggregatedHttpResponse res = discoveryClient.get("/discovery").aggregate().join();

            final ArrayList<Endpoint> ips = new ArrayList<>();
            for (String address : csvSplitter.split(res.contentUtf8())) {
                final Iterator<String> iterator = colonSplitter.split(address).iterator();
                final String ip = iterator.next();
                final int port = Integer.parseInt(iterator.next());
                ips.add(Endpoint.of(ip, port));
            }

            final EndpointGroup endpointGroup = EndpointGroup.of(ips);
            final HealthCheckedEndpointGroup healthCheckedEndpointGroup =
                    HealthCheckedEndpointGroup.builder(endpointGroup, "/health").build();
            client = WebClient.builder(SessionProtocol.HTTP, healthCheckedEndpointGroup)
                              .responseTimeoutMillis(0)
                              .build();
        }

        @Override
        public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
            return client.execute(req);
        }
    }
}
