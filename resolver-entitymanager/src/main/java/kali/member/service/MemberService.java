package kali.member.service;

import kali.member.dto.MemberUpdateRequest;
import kali.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class MemberService {

    public void modify(MemberUpdateRequest request, Member member) {
        member.updateName(request.name());
    }
}
