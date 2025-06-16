## 주제
@RequestBody vs @ModelAttribute


### 알게 된 사실
- 기본 생성자가 없어도 역직렬화가 가능한 경우가 있다
  - ackson이 리플렉션을 통해 생성자 파라미터 정보를 추론하기 때문일까?


### 개념
#### 리플렉션
- 자바에서 클래스, 필드, 메서드, 생성자 등의 메타데이터에 런타임에 접근하고 조작할 수 있는 기능
- Java 8 이전까지는 생성자 파라미터 이름을 리플렉션으로 알 수 없었지만,
  Java 8 이상에서는 컴파일 시 -parameters 옵션을 사용하면
  생성자 파라미터 이름 정보를 .class 파일에 포함시킬 수 있다
- 이 정보를 활용하면 Jackson이 기본 생성자 없이도 생성자 기반 역직렬화를 수행할 수 있다


출처
- https://colour-my-memories-blue.tistory.com/16
- https://tecoble.techcourse.co.kr/post/2021-05-11-requestbody-modelattribute/
- https://www.maeil-mail.kr/question/14
