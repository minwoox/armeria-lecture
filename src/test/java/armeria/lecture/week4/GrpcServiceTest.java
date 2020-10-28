package armeria.lecture.week4;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

class GrpcServiceTest {

    @RegisterExtension
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {

        }
    };

    @Test
    void blockingStub() {
    }

    @Test
    void stub() throws InterruptedException {
    }

    @Test
    void futureStub() throws Exception {
    }

    @Test
    void helloStream() throws InterruptedException {
    }

    @Test
    void helloPayload() throws Exception {
    }
}
