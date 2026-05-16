package kali.requestbody.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JacksonDeserializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("케이스 2: final 여부에 따른 역직렬화 전략 차이")
    class FinalFieldStrategyTest {

        @BeforeEach
        void resetFlags() {
            NonFinalTwoConstructorDto.defaultConstructorCalled = false;
            NonFinalTwoConstructorDto.argsConstructorCalled = false;
            FinalTwoConstructorDto.defaultConstructorCalled = false;
            FinalTwoConstructorDto.argsConstructorCalled = false;
        }

        @DisplayName("시나리오 2-1: non-final 필드 → 기본 생성자로 빈 객체 생성 후 field reflection으로 값 주입")
        @Test
        void nonFinal_필드면_기본생성자를_사용한다() throws Exception {
            // given
            String json = """
                    {"name": "kali", "age": 25}
                    """;

            // when
            NonFinalTwoConstructorDto result = objectMapper.readValue(json, NonFinalTwoConstructorDto.class);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.getName()).isEqualTo("kali");
                softly.assertThat(result.getAge()).isEqualTo(25);
                softly.assertThat(NonFinalTwoConstructorDto.defaultConstructorCalled).isTrue();
                softly.assertThat(NonFinalTwoConstructorDto.argsConstructorCalled).isFalse();
            });
        }

        @DisplayName("시나리오 2-2: final 필드여도 기본 생성자를 사용한다 (리플렉션으로 final 필드에 값 주입)")
        @Test
        void final_필드여도_기본생성자를_사용한다() throws Exception {
            // given
            String json = """
                    {"name": "kali", "age": 25}
                    """;

            // when
            FinalTwoConstructorDto result = objectMapper.readValue(json, FinalTwoConstructorDto.class);

            // then - final 필드여도 기본 생성자 사용 + field reflection으로 값 주입
            assertSoftly(softly -> {
                softly.assertThat(result.getName()).isEqualTo("kali");
                softly.assertThat(result.getAge()).isEqualTo(25);
                softly.assertThat(FinalTwoConstructorDto.defaultConstructorCalled).isTrue();
                softly.assertThat(FinalTwoConstructorDto.argsConstructorCalled).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("케이스 3: final 필드 + 인자 있는 생성자 + getter 없음")
    class FinalFieldNoGetterTest {

        @DisplayName("역직렬화는 성공하지만 직렬화하면 예외가 발생한다 (getter 없으므로 직렬화할 프로퍼티가 없음)")
        @Test
        void 역직렬화_성공_직렬화_예외() throws Exception {
            // given
            String json = """
                    {"name": "kali", "age": 25}
                    """;

            // when - 역직렬화 성공 확인
            FinalFieldNoGetterDto deserialized = objectMapper.readValue(json, FinalFieldNoGetterDto.class);

            // then - 객체는 생성됨
            assertThat(deserialized).isNotNull();

            // when & then - 직렬화하면 InvalidDefinitionException 발생
            assertThatThrownBy(() -> objectMapper.writeValueAsString(deserialized))
                    .isInstanceOf(InvalidDefinitionException.class);
        }
    }

    @Nested
    @DisplayName("케이스 4: Field Reflection으로 final 필드에 값 쓰기")
    class FieldReflectionTest {

        @DisplayName("final 필드에 리플렉션으로 값을 쓸 수 있는가 — 기술적으로 가능하다")
        @Test
        void final_필드에_리플렉션으로_값을_쓸_수_있는가() throws Exception {
            // given
            FinalFieldDto dto = new FinalFieldDto("원래값", 1L);

            // when
            Field nameField = FinalFieldDto.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(dto, "변경된값");

            // then - 기술적으로 가능함을 증명
            assertThat(nameField.get(dto)).isEqualTo("변경된값");
        }
    }
}
