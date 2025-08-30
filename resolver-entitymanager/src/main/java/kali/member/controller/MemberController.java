package kali.member.controller;

import kali.member.dto.MemberUpdateRequest;
import kali.member.entity.Member;
import kali.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@RequestMapping("/members")
@Controller
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Void> test(@RequestBody MemberUpdateRequest request, Member member) {
        memberService.modify(request, member);
        return ResponseEntity.ok().build();
    }
}
