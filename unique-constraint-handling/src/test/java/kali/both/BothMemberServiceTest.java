package kali.both;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

// 동시 요청을 보려면 트랜잭션이 실제로 커밋돼야 해서 @DataJpaTest(테스트마다 롤백) 대신 @SpringBootTest를 쓴다
@SpringBootTest
class BothMemberServiceTest {

    @Autowired
    private BothMemberService memberService;

    @MockitoSpyBean
    private BothMemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAllInBatch();
    }

    @DisplayName("이미 가입된 이메일은 DB까지 가기 전에 코드 검사에서 막힌다")
    @Test
    void join_alreadyJoined() {
        // given
        memberService.join("kali@test.com", "칼리");

        // when & then: 원인 예외(cause)가 없다 = DB 예외가 아니라 코드 검사에서 던졌다
        assertThatThrownBy(() -> memberService.join("kali@test.com", "다른 칼리"))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 가입된 이메일입니다: kali@test.com")
            .hasNoCause();
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

    @DisplayName("두 요청이 모두 코드 검사를 통과해도, DB가 하나만 받아주고 나머지는 같은 커스텀 예외로 바뀐다")
    @Test
    void join_concurrently() throws Exception {
        // given: 방식 1 테스트와 같은 순서(검사 → 검사 → 저장 → 저장)를 만든다
        Answer<?> realRepository = mockingDetails(memberRepository).getMockCreationSettings().getDefaultAnswer();
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object notJoined = realRepository.answer(invocation);
            bothChecked.await(5, TimeUnit.SECONDS);
            return notJoined;
        }).when(memberRepository).existsByEmail(anyString());
        Callable<Object> join = () -> memberService.join("kali@test.com", "칼리");

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Object>> results = List.of(executor.submit(join), executor.submit(join));
        List<Throwable> failures = new ArrayList<>();
        for (Future<Object> result : results) {
            try {
                result.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        executor.shutdown();

        // then: 실패한 쪽은 DB 예외를 원인으로 가진다 = 코드 검사를 빠져나가 DB에서 막혔다
        assertThat(memberRepository.countByEmail("kali@test.com")).isEqualTo(1);
        assertThat(failures).singleElement()
            .isInstanceOf(DuplicateEmailException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("이름이 비어서 난 예외는 이메일 중복으로 바꾸지 않는다")
    @Test
    void join_nullName() {
        // when & then
        assertThatThrownBy(() -> memberService.join("kali@test.com", null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(DuplicateEmailException.class);
    }
}
