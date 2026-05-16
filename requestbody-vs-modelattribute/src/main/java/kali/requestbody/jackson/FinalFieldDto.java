package kali.requestbody.jackson;

import lombok.Getter;

@Getter
public class FinalFieldDto {

    private final String name;
    private final long age;

    public FinalFieldDto(String name, long age) {
        this.name = name;
        this.age = age;
    }
}
