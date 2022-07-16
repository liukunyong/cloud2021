package com.atguigu.springcloud.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优点：spring框架自带的定时功能，springboot做了非常好的封装，开启和定义定时任务非常容易，
 * 支持复杂的cron表达式，可以满足绝大多数单机版的业务场景。单个任务时，当前次的调度完成后，再执行下一次任务调度。
 *
 * 缺点：默认单线程，如果前面的任务执行时间太长，对后面任务的执行有影响。不支持集群方式部署，
 * 不能做数据存储型定时任务
 */
//@Component
public class SpringTaskTest {
    @Scheduled(cron = "*/10 * * * * ?")
    public void test(){
        int i = 0;
        System.out.println("SpringTaskTest"+i);
        i++;
    }
}
