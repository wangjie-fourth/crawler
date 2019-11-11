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

    String getNextLink(String sql) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content);


}
