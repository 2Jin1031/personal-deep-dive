package kali.both;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BothMemberService {

    private final BothMemberRepository memberRepository;

    @Transactional
    public BothMember join(String email, String name) {
        validateNotJoined(email);
        try {
            return memberRepository.save(new BothMember(email, name));
        } catch (DataIntegrityViolationException e) {
            // 조회와 저장 사이에 다른 요청이 먼저 저장한 경우만 여기로 온다
            if (isEmailDuplicate(e)) {
                throw new DuplicateEmailException("이미 가입된 이메일입니다: " + email, e);
            }
            throw e;
        }
    }

    @Transactional
    public void joinAll(List<JoinRequest> requests) {
        validateNoDuplicateInRequests(requests);
        for (JoinRequest request : requests) {
            join(request.email(), request.name());
        }
    }

    private void validateNotJoined(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다: " + email);
        }
    }

    private void validateNoDuplicateInRequests(List<JoinRequest> requests) {
        Map<String, Integer> firstLineByEmail = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            String email = requests.get(i).email();
            Integer firstLine = firstLineByEmail.putIfAbsent(email, i + 1);
            if (firstLine != null) {
                throw new DuplicateEmailException(
                    "요청 목록 " + firstLine + "번째 줄과 " + (i + 1) + "번째 줄의 이메일이 같습니다: " + email);
            }
        }
    }

    private boolean isEmailDuplicate(DataIntegrityViolationException e) {
        if (!(e.getCause() instanceof ConstraintViolationException violation)) {
            return false;
        }
        String constraintName = violation.getConstraintName();
        return constraintName != null
            && constraintName.toLowerCase().contains(BothMember.EMAIL_UNIQUE_CONSTRAINT);
    }
}
