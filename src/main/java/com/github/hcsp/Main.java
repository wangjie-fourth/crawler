package com.github.hcsp;

import com.github.hcsp.dao.CrawlerDao;
import com.github.hcsp.dao.impl.MybatisCrawlerDao;

/**
 * @ClassName Main
 * @Description
 * @Author 25127
 * @Date 2019/11/13 23:18
 * @Email jie.wang13@hand-china.com
 **/
public class Main {
    public static void main(String[] args) {
        CrawlerDao dao = new MybatisCrawlerDao();

        for (int i = 0; i < 8; i++) {
            // 由于数据库是一个天然的同步，所以这里不用考虑锁的问题
            new Crawler(dao).start();
        }

    }
}
