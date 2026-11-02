## 주제
"이메일은 중복 불가" 같은 유니크 조건을 어디서 처리할까?

1. 코드에서 if 문으로 직접 중복 검사 → 커스텀 예외 (`appcheck`)
2. DB 유니크 제약에 맡기고, DB가 던진 예외를 받아 커스텀 예외로 변환 (`dbcatch`)
3. 둘 다 (`both`)

같은 회원 가입 기능을 세 방식으로 만들고, 같은 상황을 테스트로 돌려서 결과를 비교했다.

- 단건 가입 `join(email, name)`
- 일괄 가입 `joinAll(목록)`: 엑셀 업로드처럼 여러 명을 한 번에 넣는 경우

### 실행
```bash
./gradlew :unique-constraint-handling:test
```
- Spring Boot 3.4.4, H2 메모리 DB (MySQL 모드). 로컬 DB 설치 없이 돌아간다
- DB 에러 메시지와 제약 이름 형식은 실제 MySQL과 다를 수 있다

### 결과 비교

| 상황 | 1. 코드 검사 | 2. DB 예외 변환 | 3. 둘 다 |
|---|---|---|---|
| 이미 가입된 이메일 | 막음. "이미 가입된 이메일입니다" | 막음. 같은 메시지 (DB까지 갔다 옴) | 막음. DB까지 안 감 |
| 요청 목록 안에 같은 이메일 | 막음. "2번째 줄과 3번째 줄" | 막음. 하지만 "이미 가입된 이메일입니다"로 잘못 안내 | 막음. "2번째 줄과 3번째 줄" |
| 동시에 같은 이메일로 가입 | **못 막음. 두 줄 저장됨** | 막음. 하나만 저장 | 막음. 하나만 저장 |
| 이름이 비어 있음 (다른 에러) | - | 중복으로 바꾸지 않고 그대로 던짐 | 중복으로 바꾸지 않고 그대로 던짐 |

일괄 가입은 세 방식 모두 하나라도 실패하면 트랜잭션이 통째로 롤백되어 0건 저장된다.

### 알게 된 사실
- 코드 검사만으로는 동시 요청을 못 막는다
  - "검사 → 검사 → 저장 → 저장" 순서가 되면 두 요청 모두 "없음"을 보고 통과한다
  - 테스트에서는 두 요청이 검사를 마칠 때까지 서로 기다리게 해서 이 순서를 일부러 만들었다 (`CyclicBarrier`)
- `DataIntegrityViolationException`은 중복 전용 예외가 아니다
  - 이름을 비워도 같은 타입으로 온다. 이 경우는 DB까지 가지도 않았다. Hibernate가 SQL을 보내기 전에 `PropertyValueException`으로 막고, Spring이 같은 타입으로 바꿔서 던졌다
  - 그래서 `catch (DataIntegrityViolationException e)`에서 전부 "이메일 중복"으로 바꾸면 틀린 안내가 된다
  - 원인 예외(`org.hibernate.exception.ConstraintViolationException`)의 제약 이름을 보고 이메일 제약일 때만 바꿨다
- 제약 이름은 직접 지어야 한다. 그리고 DB가 돌려주는 이름은 내가 지은 이름 그대로가 아니다
  - 지은 이름: `uk_db_catch_member_email`
  - H2가 돌려준 이름: `PUBLIC.UK_DB_CATCH_MEMBER_EMAIL_INDEX_C`
  - 그래서 `equals` 대신 소문자로 바꿔 `contains`로 비교했다
  - 이름을 안 지으면 Hibernate가 `UK` + 해시값 같은 이름을 붙여서 코드에서 알아보기 어렵다
- DB 예외 변환만 쓰면 "요청 목록 안의 중복"과 "이미 있는 데이터와의 중복"을 구분하지 못한다
  - DB 입장에서는 둘 다 같은 유니크 위반이다
  - 몇 번째 줄이 문제인지 알려주려면 DB에 가기 전에 코드에서 목록을 검사해야 한다
- 둘 다 쓰면 DB 예외 쪽 catch는 "검사와 저장 사이에 다른 요청이 끼어든 경우"에만 탄다
  - 테스트에서 실패한 쪽 예외의 원인(cause)이 `DataIntegrityViolationException`인지로 확인했다

### 어떤 걸 고를까
- 판단 근거
  - 끝까지 막아주나: 코드 검사만으로는 동시 요청이 샌다
  - 에러 메시지를 얼마나 구체적으로 줄 수 있나: 코드 검사가 유리하다 (몇 번째 줄인지 등)
  - 동시에 들어올 일이 있나: 사용자가 버튼을 연타할 수 있는 기능이면 DB 제약이 꼭 필요하다
  - 추가 비용: 코드 검사는 조회가 한 번 늘고, DB 예외 변환은 제약 이름 비교 코드가 생긴다
- 정리
  - DB 제약은 거의 항상 건다. 동시 요청과 코드 밖 경로(직접 SQL 등)를 막는 마지막 방어선이다
  - 사용자에게 친절한 메시지가 필요하면 코드 검사를 앞에 둔다 (방식 3)
  - 관리자 기능처럼 동시 요청이 거의 없고 입력이 목록이면, 목록 검사(코드)와 DB 제약만 두고 DB 예외는 굳이 변환하지 않아도 된다

### 개념
#### 유니크 제약 (UNIQUE)
- 컬럼(또는 컬럼 묶음)에 같은 값이 두 번 들어가지 못하게 DB가 막는 규칙
- 어떤 경로로 들어오든 막는다. 동시에 들어와도 하나만 받아준다

#### `@UniqueConstraint`
- 엔티티에 붙이는 JPA 어노테이션. 런타임에 Hibernate가 중복을 검사해 주지는 않는다
- `ddl-auto`로 테이블을 만들 때 UNIQUE DDL이 된다
  ```
  create table db_catch_member (..., constraint uk_db_catch_member_email unique (email))
  ```
- Flyway처럼 마이그레이션으로 스키마를 관리하면 마이그레이션에도 따로 적어야 한다. 어노테이션만으로는 운영 DB에 제약이 안 생긴다

#### save() 안에서 DB 예외를 잡을 수 있는 이유
- `GenerationType.IDENTITY`라서 `save()` 시점에 INSERT가 바로 나간다 (장소2에서 확인한 내용)
- 그래서 `save()`를 감싼 try-catch에서 유니크 위반을 잡을 수 있었다
- 쓰기 지연되는 전략이었다면 INSERT가 flush나 커밋 때 나가서, 서비스 안 catch로는 못 잡을 수 있다 (이 프로젝트에서는 테스트 안 함)

### 테스트하면서 막힌 것
- 동시 요청은 트랜잭션이 실제로 커밋돼야 보인다. 그래서 테스트마다 롤백하는 `@DataJpaTest` 대신 `@SpringBootTest`를 쓰고, 테스트마다 테이블을 비웠다
- Spring Data 레포지토리를 `@MockitoSpyBean`으로 감싸고 `callRealMethod()`를 부르면 `Cannot call abstract real method` 에러가 난다
  - 레포지토리가 인터페이스 프록시라서다
  - 스파이의 기본 동작(진짜 레포지토리에 위임)을 꺼내서 불렀다: `mockingDetails(repository).getMockCreationSettings().getDefaultAnswer()`
