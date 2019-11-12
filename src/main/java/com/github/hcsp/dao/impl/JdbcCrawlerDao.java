package com.github.hcsp.dao.impl;


import com.github.hcsp.dao.CrawlerDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * @ClassName DataAccessObject
 * @Description
 * @Author 25127
 * @Date 2019/11/11 23:18
 * @Email jie.wang13@hand-china.com
 **/
public class JdbcCrawlerDao implements CrawlerDao {

    private final Connection connection;

    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:/F:/ideaWorks/xiedaimala/project/crawler/database/news");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private String getNextLink(String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            updateDatabase(link, "delete from links_to_be_processed where link = ?");
        }
        return link;
    }


    private void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where LINK = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) {
        try (PreparedStatement statement = connection.prepareStatement("insert into news(url,TITLE,CONTENT,CREATED_AT,MODIFIED_AT) values ( ?,?,?,now(),now())")) {
            statement.setString(1, link);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void insertProcessedLink(String link) {

    }

    @Override
    public void insertLinkToBeProcessed(String href) {

    }


}
