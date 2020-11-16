
/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-16
 */
public class SyncTest {

    public static void main(String args[]) throws Exception{

        for(int i=0;i<100;i++){
            new Thread(new Runnable() {
                int k = 0;
                @Override
                public void run() {
                    while (true) {
                        //多线程竞争下：
                        //futex(0x7fd26013ea54, FUTEX_WAKE_OP_PRIVATE, 1, 1, 0x7fd26013ea50, {FUTEX_OP_SET, 0, FUTEX_OP_CMP_GT, 1}) = 1
                        //write(1, "92", 2)

                        //单一线程下:
                        synchronized (String.class) {
                            k++;
                            System.out.println(k);
                        }
                    }
                }
            }).start();
        }
    }
}
