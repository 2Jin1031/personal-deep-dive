package kali.requestbody.jackson;

import lombok.Getter;

@Getter
public class NonFinalTwoConstructorDto {

    private String name;
    private long age;

    public static boolean defaultConstructorCalled = false;
    public static boolean argsConstructorCalled = false;

    public NonFinalTwoConstructorDto() {
        defaultConstructorCalled = true;
    }

    public NonFinalTwoConstructorDto(String name, long age) {
        argsConstructorCalled = true;
        this.name = name;
        this.age = age;
    }
}
