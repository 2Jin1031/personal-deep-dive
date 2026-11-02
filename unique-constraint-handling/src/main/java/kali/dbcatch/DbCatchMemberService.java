package kali.dbcatch;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DbCatchMemberService {

    private final DbCatchMemberRepository memberRepository;

    @Transactional
    public DbCatchMember join(String email, String name) {
        try {
            // IDENTITY 전략이라 save() 안에서 INSERT가 바로 나간다. 그래서 여기서 DB 예외를 잡을 수 있다
            return memberRepository.save(new DbCatchMember(email, name));
        } catch (DataIntegrityViolationException e) {
            if (isEmailDuplicate(e)) {
                throw new DuplicateEmailException("이미 가입된 이메일입니다: " + email, e);
            }
            throw e;
        }
    }

    @Transactional
    public void joinAll(List<JoinRequest> requests) {
        for (JoinRequest request : requests) {
            join(request.email(), request.name());
        }
    }

    // DataIntegrityViolationException은 중복 말고도(예: NOT NULL 위반) 같은 타입으로 온다. 제약 이름으로 골라낸다
    private boolean isEmailDuplicate(DataIntegrityViolationException e) {
        if (!(e.getCause() instanceof ConstraintViolationException violation)) {
            return false;
        }
        String constraintName = violation.getConstraintName();
        return constraintName != null
            && constraintName.toLowerCase().contains(DbCatchMember.EMAIL_UNIQUE_CONSTRAINT);
    }
}
