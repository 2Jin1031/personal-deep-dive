package kali.both;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BothMemberRepository extends JpaRepository<BothMember, Long> {

    boolean existsByEmail(String email);

    long countByEmail(String email);
}
