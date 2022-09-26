package com.atguigu.springcloud.task;//package com.atguigu.springcloud.task;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.util.Timer;
//import java.util.TimerTask;
//
///**
// * 由于 Timer 是单线程执行任务，如果其中一个任务耗时非常长，会影响其他任务的执行。
// * 如果 TimerTask 抛出 RuntimeException ，Timer会停止所有任务的运行。
// */
////@Component
//@Order(value = 1)
//@Slf4j
//public class useTimer implements ApplicationRunner {
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        log.info("执行时间"+new Date());
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            int i = 0;
//            @Override
//            public void run() {
//                System.out.println("useTimer"+i);
//                i++;
//            }
//        },10,5000);
//    }
//}
