import com.alibaba.csp.sentinel.*;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-13
 */
public class SentinelTest {


    private void initFlowQpsRule () {
        List<DegradeRule> rules = new ArrayList<>();
        DegradeRule rule = new DegradeRule();
        rule.setResource("helloWorld");
        // set threshold RT, 10 ms
        rule.setCount(0.5); //50%以上异常
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO); //统计异常比例
        rule.setTimeWindow(5); //熔断时长，单位秒
        rule.setStatIntervalMs(1000*5); //统计时长10秒
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);
    }

    @Test
    public void sentinel() throws Exception{
        initFlowQpsRule();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Entry entry = null;
                try {

                    /*entry = SphO.entry("helloWrold"); Sph0获取资源失败，返回false，内部捕获所有异常*/
                    entry = SphU.entry("helloWorld"); //SphU获取资源失败，抛出BlockException异常
                    System.out.println("do work");
                    throw new RuntimeException("主动异常");

                } catch (Throwable t) {
                    if (!BlockException.isBlockException(t)) {
                        Tracer.trace(t);
                        System.out.println("记录异常!");
                    } else {
                        System.out.println("block!");
                    }
                    }finally{
                        if (entry != null) {
                            entry.exit();
                        }
                    }
                }
            };

        for(int i=0;i<20;i++){
            try {
                System.out.println(i);
                runnable.run();
                TimeUnit.SECONDS.sleep(1);
            }catch (Exception e){

            }

        }

        System.in.read();
    }


}
