package kali.both;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 방식 3: 코드 검사로 친절한 메시지를 먼저 주고, 코드 검사를 빠져나간 경우는 DB 유니크 제약이 막는다
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "both_member",
    uniqueConstraints = @UniqueConstraint(name = BothMember.EMAIL_UNIQUE_CONSTRAINT, columnNames = "email")
)
public class BothMember {

    public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_both_member_email";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    public BothMember(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
