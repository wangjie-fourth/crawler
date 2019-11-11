package com.github.hcsp;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ClassName Demo
 * @Description
 * @Author 25127
 * @Date 2019/11/10 23:37
 * @Email jie.wang13@hand-china.com
 **/
public class Demo {
    static int i = 0;

    public static void main(String[] args) {
        try {
            new Thread(() -> {
                throw new RuntimeException();
            }).start();

            doSomething();
        } catch (Exception e) {
            // 这里只能捕捉到当前线程跑出来的异常；上面那个新线程的异常是无法抛到这里的
        }
    }

    private static void doSomething() {
        i++;
    }
}
