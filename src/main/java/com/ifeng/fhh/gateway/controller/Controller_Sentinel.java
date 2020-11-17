package com.ifeng.fhh.gateway.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-16
 */
//@Controller
//@ResponseBody
public class Controller_Sentinel {


    /*private static final Logger LOGGER = LoggerFactory.getLogger(Controller_Sentinel.class);

    private static void initFlowQpsRule () {
        List<DegradeRule> rules = new ArrayList<>();
        DegradeRule rule = new DegradeRule();
        rule.setResource("api");
        // set threshold RT, 10 ms
        rule.setCount(0.5); //50%以上异常
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO); //统计异常比例
        rule.setTimeWindow(5); //熔断时长，单位秒
        rule.setStatIntervalMs(1000*2); //统计时长2秒
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);
    }

    static {
        initFlowQpsRule();
    }


    @RequestMapping("/st")
    public Mono<String> testResilience4j(){

        LOGGER.info("st : {}", Thread.currentThread().getName());

        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Entry entry = null;
        try {
            //entry = SphO.entry("helloWrold"); Sph0获取资源失败，返回false，内部捕获所有异常
            entry = SphU.entry("api"); //SphU获取资源失败，抛出BlockException异常
            completableFuture.complete("ok");
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
        Mono<String> mono = Mono.fromFuture(completableFuture);
        return mono;
    }*/
}
