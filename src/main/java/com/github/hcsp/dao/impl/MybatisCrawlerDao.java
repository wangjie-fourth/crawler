package com.github.hcsp.dao.impl;

import com.github.hcsp.dao.CrawlerDao;
import com.github.hcsp.domain.News;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName MybatisCrawlerDao
 * @Description
 * @Author 25127
 * @Date 2019/11/12 21:39
 * @Email jie.wang13@hand-china.com
 **/
public class MybatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkThenDelete() throws SQLException {
        String url = null;
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            url = sqlSession.selectOne("com.github.hcsp.MyMapper.selectNextAvailableLink");
            if (url != null) {
                sqlSession.delete("com.github.hcsp.MyMapper.deleteLink", url);
            }
        }
        return url;
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            int count = sqlSession.selectOne("com.github.hcsp.MyMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertNews", new News(link, content, title));
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_already_processed");
        param.put("link", link);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertLink", param);
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "links_to_be_processed");
        param.put("link", link);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            sqlSession.insert("com.github.hcsp.MyMapper.insertLink", param);
        }
    }
}
