package kali;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import kali.basicPersistence.member.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MemberPersistenceTest {

    @Test
    @DisplayName("persist() 후 commit 전에는 DB에서 조회되지 않고, commit 후에는 조회된다")
    void persist_commit_then_find_from_db() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-unit");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        // given
        Member member = new Member(1L, "name1");

        // when
        tx.begin();
        em.persist(member);

        // 새로운 EntityManager로 DB에서 조회 시도 (commit 전)
        EntityManager tempEmBeforeCommit = emf.createEntityManager();
        Member foundBeforeCommit = tempEmBeforeCommit.find(Member.class, 1L);

        // then
        Assertions.assertThat(foundBeforeCommit).isNull();

        System.out.println("===============");
        tx.commit();

        // 새로운 EntityManager로 다시 조회 시도 (commit 후)
        EntityManager tempEmAfterCommit = emf.createEntityManager();
        Member foundAfterCommit = tempEmAfterCommit.find(Member.class, 1L);

        Assertions.assertThat(foundAfterCommit).isNotNull();
        Assertions.assertThat(foundAfterCommit.getName()).isEqualTo("name1");

        tempEmBeforeCommit.close();
        tempEmAfterCommit.close();
        em.close();
        emf.close();
    }
}
