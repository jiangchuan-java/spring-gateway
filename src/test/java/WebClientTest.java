import com.ifeng.fhh.gateway.util.httpclient.ApacheAsyncHttpClient;
import com.ifeng.fhh.gateway.util.httpclient.HttpClientTemplate;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public class WebClientTest {

    @Test
    public void test() throws Exception{
        //return FutureMono.from(channel().writeAndFlush(newFullBodyMessage(Unpooled.EMPTY_BUFFER)));
    }

    @Test
    public void testApacheHttpClient() throws Exception {
        HttpClientTemplate template = new ApacheAsyncHttpClient();


            Mono<String> mono = template.get("http://local.fhh-gateway.ifeng.com/zmt-service/account/enumList");

            mono.subscribe(resp -> {
                System.out.println(resp);
            });

            TimeUnit.MICROSECONDS.sleep(200);


       // System.in.read();
    }


}
