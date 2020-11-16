import java.util.concurrent.atomic.AtomicLong;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-16
 */
public class AtomicTest {

    public static void main(String args[]) throws Exception{

        AtomicLong atomicLong = new AtomicLong(0);

        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                int k = 0;
                @Override
                public void run() {
                    while (true) {
                        //futex(0x7f203c148954, FUTEX_WAKE_OP_PRIVATE, 1, 1, 0x7f203c148950, {FUTEX_OP_SET, 0, FUTEX_OP_CMP_GT, 1}) = 1
                        //write(1, "atomic257", 9)
                        atomicLong.getAndIncrement();
                        System.out.println("atomic"+ atomicLong.get());
                    }
                }
            }).start();
        }
    }
}
