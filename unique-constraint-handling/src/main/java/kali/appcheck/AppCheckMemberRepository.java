package kali.appcheck;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppCheckMemberRepository extends JpaRepository<AppCheckMember, Long> {

    boolean existsByEmail(String email);

    long countByEmail(String email);
}
