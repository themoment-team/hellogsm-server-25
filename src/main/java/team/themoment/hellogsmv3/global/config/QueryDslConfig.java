package team.themoment.hellogsmv3.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryDslConfig {

  @PersistenceContext private EntityManager entityManager;

  @Bean
  protected JPAQueryFactory queryFactory() {
    return new JPAQueryFactory(entityManager);
  }
}
