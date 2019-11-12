package com.github.hcsp.dao;

import java.sql.SQLException;

/**
 * @ClassName CrawlerDao
 * @Description
 * @Author 25127
 * @Date 2019/11/11 23:50
 * @Email jie.wang13@hand-china.com
 **/
public interface CrawlerDao {

    String getNextLinkThenDelete() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content);

    void insertProcessedLink(String link);

    void insertLinkToBeProcessed(String href);
}
