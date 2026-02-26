package kali.member.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import kali.member.dto.MemberUpdateRequest;
import kali.member.entity.Member;
import kali.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MemberService.class)
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("멤버의 닉네임이 수정된다")
    @Test
    void update_success() {
        // given
        Member member = new Member(null, "kali");
        Member savedMember = memberRepository.save(member);

        MemberUpdateRequest request = new MemberUpdateRequest("updatedKali");

        // when
        memberService.modify(request, savedMember);

        // then
        Member actual = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("updatedKali");
    }
}
