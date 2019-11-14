package com.github.hcsp;

import com.github.hcsp.domain.News;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * @ClassName MockDataGenerate
 * @Description 用于生产很多虚拟数据
 * @Author 25127
 * @Date 2019/11/13 23:40
 * @Email jie.wang13@hand-china.com
 **/
public class MockDataGenerate {

    private static final int TARGET_ROW_COUNT = 100_0000;

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = sqlSession.selectList("com.github.hcsp.MockMapper.selectNews");

            int count = TARGET_ROW_COUNT - 950000;
            Random random = new Random();

            try {
                while (count-- > 0) {
                    int index = random.nextInt(currentNews.size() - 1);
                    News newsToBeInserted = currentNews.get(index);

                    Instant currentTime = newsToBeInserted.getCreatedAt();
                    currentTime = currentTime.minusSeconds(random.nextInt(3600 * 24 * 365));
                    newsToBeInserted.setCreatedAt(currentTime);
                    newsToBeInserted.setModifiedAt(currentTime);

                    sqlSession.insert("com.github.hcsp.MockMapper.insertNews", newsToBeInserted);
                    System.out.println(count);

                    if (count % 2000 == 0){
                        sqlSession.commit();
                    }
                }
                sqlSession.commit();
            } catch (Exception e) {
                sqlSession.rollback();
                throw new RuntimeException(e);
            }
        }

    }
}
