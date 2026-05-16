## 주제
@RequestBody vs @ModelAttribute


### 알게 된 사실
- 기본 생성자가 없어도 역직렬화가 가능한 경우가 있다
    - `-parameters` 컴파일 옵션 덕분에 Jackson이 생성자 파라미터 이름을 알 수 있어서 자동 매핑됨
- Jackson은 기본 생성자가 있으면 **final 여부와 관계없이** 무조건 기본 생성자를 우선 사용한다
- final 필드여도 리플렉션(`setAccessible(true)` + `Field.set()`)으로 값 주입이 가능하며, Jackson도 실제로 이 방법을 사용한다
- Jackson의 생성자 선택 우선순위:
    1. `@JsonCreator`가 붙은 생성자
    2. 기본 생성자 (존재하면 무조건 사용, final 여부 무관)
    3. 기본 생성자가 없고 인자 있는 생성자가 하나뿐이면 그것을 사용 (`-parameters` 필요)

### 개념
#### 리플렉션
- 자바에서 클래스, 필드, 메서드, 생성자 등의 메타데이터에 런타임에 접근하고 조작할 수 있는 기능
- Java 8 이전까지는 생성자 파라미터 이름을 리플렉션으로 알 수 없었지만,
  Java 8 이상에서는 컴파일 시 -parameters 옵션을 사용하면
  생성자 파라미터 이름 정보를 .class 파일에 포함시킬 수 있다
- 이 정보를 활용하면 Jackson이 기본 생성자 없이도 생성자 기반 역직렬화를 수행할 수 있다
- `Field.setAccessible(true)` + `Field.set()`으로 final 필드에도 값을 쓸 수 있다

### Deep Dive 문서
- [Jackson 역직렬화 Deep Dive](./JACKSON_DESERIALIZATION_DEEP_DIVE.md)


출처
- https://colour-my-memories-blue.tistory.com/16
- https://tecoble.techcourse.co.kr/post/2021-05-11-requestbody-modelattribute/
- https://www.maeil-mail.kr/question/14
