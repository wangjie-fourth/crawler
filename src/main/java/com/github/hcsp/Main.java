package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName Main
 * @Description
 * @Author 25127
 * @Date 2019/10/19 22:50
 * @Email jie.wang13@hand-china.com
 **/
public class Main {
    private static String getNextLink(Connection connection, String sql) throws SQLException {
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

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/F:/ideaWorks/xiedaimala/project/crawler/database/news");


//        try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_TO_BE_PROCESSED (link) values (?)")) {
//            statement.setString(1, "https://sina.cn/");
//            statement.executeUpdate();
//        }

        String link;
        // 从数据库拿出一个链接，然后删除它，再处理这个链接
        while ((link = getNextLinkThenDelete(connection)) != null) {
            // 询问数据库，当前链接是不是已经被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                storeIntoDatabaseIfItIsNewsPagr(connection, doc, link);
                updateDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");

            } else {
                continue;
            }


        }

    }

    private static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLink(connection, "select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            updateDatabase(connection, link, "delete from links_to_be_processed where link = ?");
        }
        return link;
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (href.equals("#")) {
                continue;
            }
            if (href.trim().equals("")) {
                continue;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                System.out.println("href = " + href);
                updateDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
            }
        }
    }


    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    // 假如这是一个新闻得详情页面，就存入数据库，否则，就什么都不做
    private static void storeIntoDatabaseIfItIsNewsPagr(Connection connection, Document doc, String link) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println("title = " + title);

                ArrayList<Element> paragraphs = articleTag.select("p");
                String content = paragraphs.stream().map(Element::text).collect(Collectors.joining());

                try (PreparedStatement statement = connection.prepareStatement("insert into news(url,TITLE,CONTENT,CREATED_AT,MODIFIED_AT) values ( ?,?,?,now(),now())")){
                    statement.setString(1,link);
                    statement.setString(2,title);
                    statement.setString(3,content);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) {
        // 这是需要处理得数据
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36");
        CloseableHttpResponse response1 = null;
        try {
            response1 = httpclient.execute(httpGet);
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link))
                && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn/".equalsIgnoreCase(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
