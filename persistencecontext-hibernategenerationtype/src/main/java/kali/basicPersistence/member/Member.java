package kali.basicPersistence.member;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Member {

    @Id
    private Long id;
    private String name;

    protected Member() {}

    public Member(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
