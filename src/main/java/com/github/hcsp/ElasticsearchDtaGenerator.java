package com.github.hcsp;

import com.github.hcsp.domain.News;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @ClassName ElasticsearchDtaGenerator
 * @Description
 * @Author 25127
 * @Date 2019/11/14 23:36
 * @Email jie.wang13@hand-china.com
 **/
public class ElasticsearchDtaGenerator {
    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<News> currentNews = getNewsFromMysql(sqlSessionFactory);

    }

    private static List<News> getNewsFromMysql(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            return sqlSession.selectList("com.github.hcsp.MockMapper.selectNews");
        }
    }
}
