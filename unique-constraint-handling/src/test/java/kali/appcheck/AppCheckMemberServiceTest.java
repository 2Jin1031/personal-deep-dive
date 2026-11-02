package kali.appcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

// 동시 요청을 보려면 트랜잭션이 실제로 커밋돼야 해서 @DataJpaTest(테스트마다 롤백) 대신 @SpringBootTest를 쓴다
@SpringBootTest
class AppCheckMemberServiceTest {

    @Autowired
    private AppCheckMemberService memberService;

    @MockitoSpyBean
    private AppCheckMemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAllInBatch();
    }

    @DisplayName("이미 가입된 이메일로 가입하면 코드에서 만든 메시지로 예외가 난다")
    @Test
    void join_alreadyJoined() {
        // given
        memberService.join("kali@test.com", "칼리");

        // when & then
        assertThatThrownBy(() -> memberService.join("kali@test.com", "다른 칼리"))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 가입된 이메일입니다: kali@test.com");
    }

    @DisplayName("요청 목록 안에 같은 이메일이 있으면 몇 번째 줄인지 알려주고, 아무것도 저장하지 않는다")
    @Test
    void joinAll_duplicateInRequests() {
        // given
        List<JoinRequest> requests = List.of(
            new JoinRequest("a@test.com", "에이"),
            new JoinRequest("kali@test.com", "칼리"),
            new JoinRequest("kali@test.com", "또 칼리")
        );

        // when & then
        assertThatThrownBy(() -> memberService.joinAll(requests))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("요청 목록 2번째 줄과 3번째 줄의 이메일이 같습니다: kali@test.com");
        assertThat(memberRepository.count()).isZero();
    }

    @DisplayName("두 요청이 모두 중복 검사를 통과한 뒤 저장하면, 같은 이메일이 두 줄 저장된다")
    @Test
    void join_concurrently() throws Exception {
        // given: 두 요청이 existsByEmail을 둘 다 통과할 때까지 서로 기다리게 해서, 검사 → 검사 → 저장 → 저장 순서를 만든다
        // Spring Data 레포지토리는 인터페이스 프록시라 callRealMethod()를 못 쓴다. 스파이의 기본 동작(진짜 레포지토리에 위임)을 그대로 부른다
        Answer<?> realRepository = mockingDetails(memberRepository).getMockCreationSettings().getDefaultAnswer();
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object notJoined = realRepository.answer(invocation);
            bothChecked.await(5, TimeUnit.SECONDS);
            return notJoined;
        }).when(memberRepository).existsByEmail(anyString());

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit(() -> memberService.join("kali@test.com", "칼리"));
        Future<?> second = executor.submit(() -> memberService.join("kali@test.com", "칼리"));
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 코드 검사만으로는 막지 못한다
        assertThat(memberRepository.countByEmail("kali@test.com")).isEqualTo(2);
    }
}
