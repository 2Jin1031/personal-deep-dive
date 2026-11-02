package kali.dbcatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

// 동시 요청을 보려면 트랜잭션이 실제로 커밋돼야 해서 @DataJpaTest(테스트마다 롤백) 대신 @SpringBootTest를 쓴다
@SpringBootTest
class DbCatchMemberServiceTest {

    @Autowired
    private DbCatchMemberService memberService;

    @Autowired
    private DbCatchMemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAllInBatch();
    }

    @DisplayName("이미 가입된 이메일로 가입하면 DB 예외를 잡아 바꾼 메시지로 예외가 난다")
    @Test
    void join_alreadyJoined() {
        // given
        memberService.join("kali@test.com", "칼리");

        // when & then
        assertThatThrownBy(() -> memberService.join("kali@test.com", "다른 칼리"))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 가입된 이메일입니다: kali@test.com")
            .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("요청 목록 안의 중복도 '이미 가입된 이메일'로 보고된다. 목록 문제인지 기존 데이터 문제인지 구분하지 못한다")
    @Test
    void joinAll_duplicateInRequests() {
        // given
        List<JoinRequest> requests = List.of(
            new JoinRequest("a@test.com", "에이"),
            new JoinRequest("kali@test.com", "칼리"),
            new JoinRequest("kali@test.com", "또 칼리")
        );

        // when & then: 3번째 줄 INSERT에서 DB가 막고, 트랜잭션이 통째로 롤백된다
        assertThatThrownBy(() -> memberService.joinAll(requests))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 가입된 이메일입니다: kali@test.com");
        assertThat(memberRepository.count()).isZero();
    }

    @DisplayName("같은 이메일로 동시에 가입하면 DB가 하나만 받아준다")
    @Test
    void join_concurrently() throws Exception {
        // given: 두 요청이 같은 순간에 출발하게 한다
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Object> join = () -> {
            start.await(5, TimeUnit.SECONDS);
            return memberService.join("kali@test.com", "칼리");
        };

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

        // then
        assertThat(memberRepository.countByEmail("kali@test.com")).isEqualTo(1);
        assertThat(failures).singleElement().isInstanceOf(DuplicateEmailException.class);
    }

    @DisplayName("이름이 비어서 난 예외도 같은 DataIntegrityViolationException이지만, 이메일 중복으로 바꾸지 않는다")
    @Test
    void join_nullName() {
        // when & then
        assertThatThrownBy(() -> memberService.join("kali@test.com", null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(DuplicateEmailException.class);
    }
}
