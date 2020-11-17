import io.prometheus.client.Counter;
import io.prometheus.client.hotspot.DefaultExports;
import org.junit.Test;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-17
 */
public class PrometheusTest {


    static final Counter requsts = Counter.build().name("fhh-gateway-request").labelNames("api").help("统计接口请求量").register();

    static {
        //gc, memory pools, classloading, and thread counts.
        DefaultExports.initialize();
    }

    @Test
    public void newRequest() {
        requsts.inc();
        System.out.println("new request");

    }
}
