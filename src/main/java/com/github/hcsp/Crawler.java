package com.github.hcsp;

import com.github.hcsp.dao.CrawlerDao;
import com.github.hcsp.dao.impl.JdbcCrawlerDao;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @ClassName Main
 * @Description
 * @Author 25127
 * @Date 2019/10/19 22:50
 * @Email jie.wang13@hand-china.com
 **/
public class Crawler {

    private static CrawlerDao dao = new JdbcCrawlerDao();

    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
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

    // 假如这是一个新闻得详情页面，就存入数据库，否则，就什么都不做
    private static void storeIntoDatabaseIfItIsNewsPagr(Document doc, String link) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println("title = " + title);

                ArrayList<Element> paragraphs = articleTag.select("p");
                String content = paragraphs.stream().map(Element::text).collect(Collectors.joining());

                dao.insertNewsIntoDatabase( link, title, content);
            }
        }
    }

    public void run() throws SQLException {
        String link;
        // 从数据库拿出一个链接，然后删除它，再处理这个链接
        while ((link = dao.getNextLinkThenDelete()) != null) {
            // 询问数据库，当前链接是不是已经被处理过了
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(doc);
                storeIntoDatabaseIfItIsNewsPagr(doc, link);
                dao.updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");

            }
        }

    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
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
                dao.updateDatabase( href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
            }
        }
    }
}
