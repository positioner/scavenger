package com.navercorp.scavenger.javaagent.collecting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
@DisplayName("MethodRegistry class")
public class MethodRegistryTest {
    MethodRegistry sut;

    @BeforeEach
    public void setUp() {
        sut = new MethodRegistry();
    }

    @Nested
    @DisplayName("getHash method")
    class GetHashMethodTest {

        @Nested
        @DisplayName("if method is cached")
        class CachedTest {
            String signature = "signature";
            String expected = "hash";

            @BeforeEach
            public void cacheMethod() {
                sut.getByteBuddySignatureToHash().put(signature, expected);
            }

            @Test
            @DisplayName("it returns cached value")
            public void cached() {
                assertThat(sut.getHash(signature, false)).isEqualTo(expected);
            }
        }

        @Nested
        @DisplayName("if method is generated by SpringCGLIB")
        class CglibTest {
            String cglibSignature = "TestClass$$EnhancerBySpringCGLIB$$hash.testMethod()";

            @Test
            @DisplayName("it returns empty string")
            public void returnNull() {
                assertThat(sut.getHash(cglibSignature, false))
                    .isEqualTo("");
            }
        }
    }

}
