package kali.requestbody.jackson;

public class FinalFieldNoGetterDto {

    private final String name;
    private final long age;

    public FinalFieldNoGetterDto(String name, long age) {
        this.name = name;
        this.age = age;
    }
}
