import org.junit.Test;

import java.util.concurrent.atomic.LongAdder;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-12
 */
public class LongAdderTest {


    @Test
    public void longAdderTest(){

        LongAdder longAdder = new LongAdder();
        longAdder.add(1);
        longAdder.sum();
    }
}
