//package com.atguigu.springcloud.task;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
///**
// * 使用 Thread 类可以做最简单的定时任务，在 run 方法中有个 while 的死循环（当然还有其他方式），执行我们自己的任务。
// *
// * 有个需要特别注意的地方是，需要用 try...catch 捕获异常，否则如果出现异常，就直接退出循环，下次将无法继续执行了。
// *
// * 这种方式做的定时任务，只能周期性执行，不能支持定时在某个时间点执行。
// *
// * 此外，该线程可以定义成 守护线程 ，在后台默默执行就好。
// */
//@Slf4j
//@Component
//@Order(value = 2)
//@Data
//public class UseThread implements ApplicationRunner {
//    private boolean falg = true;
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        log.info("定时任务开启。。。。。。"+new Date());
//        new Thread(()->{
//            int i = 0;
//            while (falg){
//                try {
//                    System.out.println("useThread"+i);
//                    Thread.sleep(5000);
//                    i++;
//                } catch (Exception e) {
//                    log.error("报错了：{}",e);
//                }
//            }
//        }).start();
//    }
//}
