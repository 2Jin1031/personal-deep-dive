package kali;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import kali.mysqlIdentity.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GenerationTypeMysqlTest {

    @Test
    @DisplayName("IDENTITY 전략에서는 persist() 직후 DB에 INSERT된다")
    void persist_with_identity_strategy_immediately_inserts_into_db() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("mysql-unit");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        tx.begin();

        Member member = new Member("user1");
        em.persist(member);// 이 시점에 INSERT 쿼리 실행됨 (IDENTITY 전략이므로 flush 강제됨)

        Long id = member.getId();
        System.out.println("id: " + id);

        System.out.println("==========================");

        String sql = "SELECT name FROM member WHERE id = ?1";
        Object result = em.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();

        Assertions.assertThat(result).isEqualTo("user1");

        tx.rollback();

        em.close();
        emf.close();
    }
}
