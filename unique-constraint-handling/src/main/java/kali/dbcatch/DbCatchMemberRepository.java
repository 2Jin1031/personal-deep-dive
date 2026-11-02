package kali.dbcatch;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DbCatchMemberRepository extends JpaRepository<DbCatchMember, Long> {

    long countByEmail(String email);
}
