package armeria.lecture.week3;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.metric.MeterIdPrefixFunction;
import com.linecorp.armeria.common.metric.PrometheusMeterRegistries;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.metric.MetricCollectingService;
import com.linecorp.armeria.server.metric.PrometheusExpositionService;

import io.micrometer.prometheus.PrometheusMeterRegistry;

public final class PrometheusExpositionServer {

    public static void main(String[] args) {
        final PrometheusMeterRegistry registry =
                PrometheusMeterRegistries.defaultRegistry();
        final Server server = Server.builder()
                                    .http(8080)
                                    .service("/", (ctx, req) -> HttpResponse.of(200))
                                    .meterRegistry(registry)
                                    .service("/prometheus", new PrometheusExpositionService(
                                            registry.getPrometheusRegistry()))
                                    .build();
        server.start().join();

    }
}
