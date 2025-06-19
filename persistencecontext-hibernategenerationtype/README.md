## 주제
MySQL에서 GenerationType.IDENTITY 전략 시 persist() 시점에서 insert 쿼리가 나갈까?


### 알게 된 사실
-  DB가 INSERT SQL를 실행하기 전에 AUTO_INCREMENT 되는 값을 알 수 없으므로 persist() 시점에서 insert 쿼리가 실행됨
   -> 특이하게 MySQL에서 GenerationType.IDENTITY 전략 시 persist() 시점에서 insert 쿼리가 나간다
### 개념
#### 영속성 컨텍스트
##### 쓰기 지연
- 평소에 `쓰기 지연 SQL 저장소`에 SQL문 모아둠
- flush() 실행 시 모아뒀던 SQL문들이 DB로 날아감
##### flush()
- DB로 SQL문이 들어가긴 하지만 rollback 가능한 수준임
- 자신의 커넥션 내에서만 확인이 가능함
#####  commit()
- 롤백할 수 없음
- 다른 커넥션에서도 DB의 변화를 확인할 수 있음

#### JPA 기본키 생성 전략 : generationType.INDENTITY
- DB에게 ID 생성 책임 전가
- MYSQL의 경우 AUTO_INCREMENT를 활용하여 생성


출처
- https://velog.io/@suk13574/JPA-%EC%98%81%EC%86%8D%EC%84%B1-%EC%BB%A8%ED%85%8D%EC%8A%A4%ED%8A%B8%EC%9D%98-%EC%A0%84%EB%B0%98%EC%A0%81%EC%9D%B8-%EC%9D%B4%ED%95%B4%EA%B0%9C%EB%85%90-%EC%9E%A5%EC%A0%90-%EB%8F%99%EC%9E%91-%EB%B0%A9
- https://devcamus.tistory.com/16#--%25--GenerationType-AUTO%25--%EC%25--%25B-%EC%25--%25----%EC%25-E%25--%EB%25-F%25--%EC%25-C%BC%EB%25A-%25-C%25--IDENTITY%25-C%25--SEQUENCE%25-C%25--TABLE%25--%25E-%25B-%AD%25--%ED%25--%25-D%25----
