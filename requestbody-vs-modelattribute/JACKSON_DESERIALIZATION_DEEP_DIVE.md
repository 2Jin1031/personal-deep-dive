# Jackson 역직렬화 Deep Dive

## 궁금한 것

기본 생성자 없이, final 필드만 있고 setter도 없는 클래스를 Jackson은 어떻게 역직렬화하는가?

구체적으로:

1. **Jackson의 역직렬화 전략 우선순위**가 실제로 동작하는가?
   - 1순위: 기본 생성자 + setter/Field Reflection → final 필드면 이 전략은 실패하는가?
   - 2순위: 인자 있는 생성자 사용 → `-parameters` 플래그 덕분에 자동으로 되는가?

2. **Field Reflection으로 final 필드에 값을 주입하는 건 가능한가?**
   - 기술적으로는 `setAccessible(true)` + `Field.set()`으로 가능하지만, Jackson이 의도적으로 안 한다는 게 사실인가?

3. **기본 생성자가 없는데 어떻게 객체를 만드는가?**
   - `-parameters` 옵션이 켜져있으면 `@JsonCreator` 없이도 생성자 파라미터를 JSON 키에 매핑할 수 있다는데, 실제로 그런가?

---

## 현재 프로젝트 상태

기존 `RequestBodyDto`는 기본 생성자 없이 인자 있는 생성자만 가진 DTO다.
하지만 필드가 `private`이지 `final`은 아니고, Lombok `@Getter`로 getter가 있다.

```java
// 현재 상태 — final 아님, setter 없음, getter 있음
@Getter
public class RequestBodyDto {
    private String name;
    private long age;
    private String password;
    private String email;

    public RequestBodyDto(String name, long age, String password, String email) {
        this.name = name;
        this.age = age;
        this.password = password;
        this.email = email;
    }
}
```

이미 "기본 생성자 없이 역직렬화 성공"은 확인했다. 여기서 더 파고들어야 할 부분이 남아있다.

---

## 실험 결과

1. **Jackson의 역직렬화 전략 우선순위** → final 필드면 이 전략은 실패하는가? → **그렇지 않다.** 기본 생성자가 있으면 final 여부와 관계없이 무조건 기본 생성자를 사용한다.
2. **Field Reflection으로 final 필드에 값을 주입하는 건 가능한가?** → Jackson이 의도적으로 안 한다는 게 사실인가? → **그렇지 않다.** Jackson은 기본 생성자로 객체를 만든 뒤 `setAccessible(true)` + `Field.set()`으로 final 필드에도 값을 주입한다.
3. **결론:** Jackson은 final 여부를 전혀 보지 않는다. 기본 생성자가 존재하면 항상 기본 생성자를 우선 사용하고, 리플렉션으로 필드에 값을 주입한다 (final이든 아니든).

---

## 예상하는 Jackson의 동작 흐름

Jackson이 JSON을 받아 객체를 만드는 전체 흐름을 단계별로 정리하면:

```
JSON 수신
  │
  ▼
대상 클래스 분석 (BeanDeserializer 생성)
  │
  ├─ 기본 생성자가 있는가?
  │    ├─ YES → 기본 생성자로 빈 객체 생성 (final 여부 무관)
  │    │         │
  │    │         ├─ setter가 있는 필드 → setter로 값 주입
  │    │         ├─ setter 없는 필드 → Field Reflection으로 값 주입
  │    │         └─ final 필드 → ✅ Field Reflection으로 값 주입 (setAccessible + Field.set)
  │    │
  │    └─ NO → 아래로
  │
  ├─ 인자 있는 생성자가 있는가?
  │    ├─ 생성자가 1개뿐 → 그 생성자 사용 시도
  │    │    ├─ -parameters 플래그로 파라미터 이름 확인 가능? → 자동 매핑
  │    │    └─ 파라미터 이름 확인 불가? → @JsonCreator + @JsonProperty 필요
  │    │
  │    ├─ 생성자가 여러 개 → @JsonCreator로 명시 필요
  │    │
  │    └─ 생성자의 파라미터에 JSON 값을 넣어 객체 생성 (final 필드도 정상 초기화됨)
  │
  └─ 어떤 생성자도 사용 불가 → 예외 발생
       "Cannot construct instance of `Xxx` (no Creators, like default constructor, exist)"
```

## 테스트로 확인하고 싶은 것

### 케이스 1: 현재 상태 확인 (이미 통과)

> 기본 생성자 없음 + private 필드 + getter 있음 → 역직렬화 성공

기존 `RequestBodyDto`와 `RequestBodyDtoTest`가 이미 이 케이스를 커버한다.
Spring Boot의 `-parameters` 옵션 덕분에 인자 있는 생성자의 파라미터 이름을 Jackson이 알 수 있어서 성공.

---

### 케이스 2: Jackson은 final 여부에 따라 역직렬화 전략을 바꾸는가?

> **검증 목표:** 기본 생성자와 인자 있는 생성자가 모두 존재할 때, 필드의 final 여부에 따라 Jackson이 다른 경로를 타는지 확인

두 시나리오 모두 **기본 생성자 + 인자 있는 생성자**가 공존하는 상황이다. 차이는 필드에 final이 붙었는지 여부뿐.

#### 시나리오 2-1: non-final 필드

> **예상:** 기본 생성자로 빈 객체 생성 → field reflection으로 값 주입 (성공)

```java
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
    // getter만 존재
}
```

#### 시나리오 2-2: final 필드

> **예상:** Jackson이 final 필드에 대한 reflection 쓰기를 회피 → 인자 있는 생성자를 사용 (성공)
>
> **실제 결과:** ❌ 예상과 다름. final 필드여도 기본 생성자를 사용하고, 리플렉션으로 final 필드에 값을 주입한다.

```java
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
    // getter만 존재
}
```

#### 검증 방법

역직렬화 후 static 플래그를 확인하여 어떤 생성자가 호출됐는지 판별한다.

- **시나리오 2-1** → `defaultConstructorCalled == true`, `argsConstructorCalled == false`
- **시나리오 2-2** → `defaultConstructorCalled == true`, `argsConstructorCalled == false` (예상과 다름!)

**실제 결과:** 두 시나리오 모두 기본 생성자를 사용한다. Jackson은 final 여부를 감지하지 않으며, 기본 생성자가 있으면 항상 기본 생성자를 우선한다.

---

### 케이스 3: final 필드 + 인자 있는 생성자 + getter 없음

> **예상: 역직렬화는 성공, 직렬화(응답)에서 빈 JSON 반환**

```java
public class FinalFieldNoGetterDto {
    private final String name;
    private final long age;

    public FinalFieldNoGetterDto(String name, long age) {
        this.name = name;
        this.age = age;
    }
    // getter 없음
}
```

역직렬화(JSON→객체)는 생성자로 하니까 되지만, 직렬화(객체→JSON)는 getter나 public 필드가 필요해서 빈 `{}`가 응답될 수 있다.
이 케이스로 역직렬화와 직렬화의 경로가 독립적이라는 것을 확인할 수 있다.

---

### 케이스 4: Field Reflection 직접 테스트

> Jackson의 동작이 아닌, **리플렉션 자체의 동작**을 순수 자바 코드로 확인

```java
@Test
void final_필드에_리플렉션으로_값을_쓸_수_있는가() throws Exception {
    FinalFieldDto dto = new FinalFieldDto("원래값", 1L);

    Field nameField = FinalFieldDto.class.getDeclaredField("name");
    nameField.setAccessible(true);
    nameField.set(dto, "변경된값");

    assertThat(nameField.get(dto)).isEqualTo("변경된값");
}
```

"기술적으로 가능한가"를 눈으로 확인하기 위한 테스트.
이 테스트가 통과하면 → 리플렉션으로 final 필드 쓰기가 가능함을 증명.

**실제 결과:** 통과한다. 그리고 Jackson도 실제로 이 방법을 사용하여 final 필드에 값을 주입한다.
