package kali.dbcatch;

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

// 방식 2: 중복은 DB 유니크 제약이 막는다. 코드는 DB가 던진 예외를 받아서 바꾸기만 한다
// 제약 이름을 직접 지어야 예외에서 "어느 제약이 걸렸는지" 알아볼 수 있다
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "db_catch_member",
    uniqueConstraints = @UniqueConstraint(name = DbCatchMember.EMAIL_UNIQUE_CONSTRAINT, columnNames = "email")
)
public class DbCatchMember {

    public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_db_catch_member_email";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    public DbCatchMember(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
