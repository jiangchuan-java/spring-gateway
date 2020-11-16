import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-12
 */
public class LongAdderTest {


    @Test
    public void longAdderTest() throws Exception{

        LongAdder longAdder = new LongAdder();
        CountDownLatch countDownLatch = new CountDownLatch(100);

        long start = System.currentTimeMillis();
        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                int k = 0;
                @Override
                public void run() {
                    while (k<100000) {
                        longAdder.add(1);
                        k++;
                    }
                    countDownLatch.countDown();
                }
            }).start();
        }

        long end = System.currentTimeMillis();
        countDownLatch.await();
        System.out.println(end-start+" "+longAdder.longValue());
    }

    @Test
    public void atomicLongTest() throws Exception{

        AtomicLong atomicLong = new AtomicLong(0);
        CountDownLatch countDownLatch = new CountDownLatch(100);

        long start = System.currentTimeMillis();
        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                int k = 0;
                @Override
                public void run() {
                    while (k<100000) {
                        atomicLong.incrementAndGet();
                        k++;
                    }
                    countDownLatch.countDown();
                }
            }).start();
        }

        long end = System.currentTimeMillis();
        countDownLatch.await();
        System.out.println(end-start+" "+atomicLong.get());
    }

    @Test
    public void syncTest() throws Exception{

        CountDownLatch countDownLatch = new CountDownLatch(100);

        A a = new A();


        long start = System.currentTimeMillis();
        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                int k = 0;
                @Override
                public void run() {
                    while (k<100000) {
                        synchronized (String.class) {
                            a.value++;
                            k++;
                        }
                    }
                    countDownLatch.countDown();
                }
            }).start();
        }

        long end = System.currentTimeMillis();
        countDownLatch.await();
        System.out.println(end-start+" "+a.value);
    }

    class A {
        public int value = 0;
    }
}
