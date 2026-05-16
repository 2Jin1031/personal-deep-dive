package kali.requestbody.jackson;

import lombok.Getter;

@Getter
public class FinalTwoConstructorDto {

    private final String name;
    private final long age;

    public static boolean defaultConstructorCalled = false;
    public static boolean argsConstructorCalled = false;

    public FinalTwoConstructorDto() {
        defaultConstructorCalled = true;
        this.name = null;
        this.age = 0;
    }

    public FinalTwoConstructorDto(String name, long age) {
        argsConstructorCalled = true;
        this.name = name;
        this.age = age;
    }
}
