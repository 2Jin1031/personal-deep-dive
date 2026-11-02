package kali.appcheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppCheckMemberService {

    private final AppCheckMemberRepository memberRepository;

    @Transactional
    public AppCheckMember join(String email, String name) {
        validateNotJoined(email);
        return memberRepository.save(new AppCheckMember(email, name));
    }

    @Transactional
    public void joinAll(List<JoinRequest> requests) {
        validateNoDuplicateInRequests(requests);
        for (JoinRequest request : requests) {
            join(request.email(), request.name());
        }
    }

    // 조회해서 이미 있으면 막는다. 조회와 저장 사이에 다른 요청이 끼어들면 둘 다 통과한다
    private void validateNotJoined(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다: " + email);
        }
    }

    // DB에 가기 전에 요청 목록 안에서 겹치는 줄을 찾는다. 몇 번째 줄인지까지 알려줄 수 있다
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
}
