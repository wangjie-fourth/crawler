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

/**
 * @ClassName Main
 * @Description
 * @Author 25127
 * @Date 2019/10/19 22:50
 * @Email jie.wang13@hand-china.com
 **/
public class Main {
    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> result = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/F:/ideaWorks/xiedaimala/project/crawler/database/news");


//        try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_TO_BE_PROCESSED (link) values (?)")) {
//            statement.setString(1, "https://sina.cn/");
//            statement.executeUpdate();
//        }


        while (true) {
            // 从数据库加载即将处理地链接地代码
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");


            if (linkPool.isEmpty()) {
                break;
            }

            // 从待处理得连接池中拿出一个链接，处理完从数据库、池子中删除
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where LINK = ?");

            // 询问数据库，当前链接是不是已经被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                storeIntoDatabaseIfItIsNewsPagr(doc);

                insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");

            } else {
                continue;
            }


        }

    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
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

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    // 假如这是一个新闻得详情页面，就存入数据库，否则，就什么都不做
    private static void storeIntoDatabaseIfItIsNewsPagr(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println("title = " + title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) {
        // 这是需要处理得数据
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
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
