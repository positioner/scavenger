package com.navercorp.scavenger.javaagent.collecting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.navercorp.scavenger.javaagent.model.Config;
import com.navercorp.scavenger.javaagent.model.Method;
import com.navercorp.scavenger.javaagent.model.Visibility;

@Nested
@DisplayName("CodeBaseScanner class")
public class CodeBaseScannerExplodedTest {
    Config config;
    CodeBaseScanner scanner;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        String file = Objects.requireNonNull(getClass().getClassLoader().getResource("scavenger-demo-0.0.1-SNAPSHOT.jar")).getFile();
        String target = file.replace("scavenger-demo-0.0.1-SNAPSHOT.jar", "scavenger-demo-0.0.1-SNAPSHOT");
        deleteDirectory(new File(target));
        Process exec = Runtime.getRuntime().exec("unzip  -o " + file + " -d " + target);
        exec.waitFor(10, TimeUnit.SECONDS);
        Properties props = new Properties();
        props.setProperty("appName", "test");
        props.setProperty("codeBase", target);
        props.setProperty("packages", "com.example.demo");
        config = new Config(props);
        scanner = new CodeBaseScanner(config);
    }

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        directoryToBeDeleted.delete();
    }

    @Nested
    @DisplayName("scan method")
    class ScanMethodTest {

        @Nested
        @DisplayName("if all methods are scanned")
        class AllMethodTest {

            @Test
            @DisplayName("it finds correct number of methods")
            public void scanAllMethod() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).hasSize(64);
            }

            @Test
            @DisplayName("it returns same codeBaseFingerprint for every scan")
            public void codeBaseFingerprint() throws IOException {
                String expectedFingerprint = scanner.scan().getCodeBaseFingerprint();
                assertThat(scanner.scan().getCodeBaseFingerprint()).isEqualTo(expectedFingerprint);
            }
        }

        @Nested
        @DisplayName("if constructor is filtered")
        class FilterConstructorTest {

            @BeforeEach
            public void setConstructorFilter() {
                config.setExcludeConstructors(true);
            }

            @Test
            @DisplayName("it does not contain constructor")
            public void scanFilterConstructor() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).map(Method::isConstructor).containsOnly(false);
            }
        }

        @Nested
        @DisplayName("if visibility filter is set to private")
        class FilterVisibilityTest {

            @BeforeEach
            public void setVisibilityFilter() {
                config.setMethodVisibility(Visibility.PRIVATE);
            }

            @Test
            @DisplayName("it finds correct number of methods")
            public void scanFilterVisibility() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).hasSize(65);
            }
        }

        @Nested
        @DisplayName("if com.example.demo.additional is excluded")
        class FilterExcludedPackagesTest {

            @BeforeEach
            public void setExcludedPackages() {
                config.setExcludePackages(Collections.singletonList("com.example.demo.additional"));
                scanner = new CodeBaseScanner(config);
            }

            @Test
            @DisplayName("it finds correct number of methods")
            public void scanFilterExcludedPackages() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).allSatisfy(e -> assertThat(e.getSignature()).doesNotContain("com.example.demo.additional"));
            }
        }

        @Nested
        @DisplayName("if @RestController is filtered")
        class FilterAnnotationTest {

            @BeforeEach
            public void setAnnotationFilter() {
                config.setAnnotations(Collections.singletonList("org.springframework.web.bind.annotation.RestController"));
            }

            @Test
            @DisplayName("it finds correct number of methods")
            public void scanFilterAnnotation() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).allSatisfy(each -> assertThat(each.getDeclaringType()).contains("Controller"));
            }

            @Nested
            @DisplayName("if com.example.demo.additional is set as an additional package and @RestController is filtered")
            class FilterAdditionalPackageTest {

                @BeforeEach
                public void setFilters() {
                    config.setAnnotations(Collections.singletonList("org.springframework.web.bind.annotation.RestController"));
                    config.setAdditionalPackages(Collections.singletonList("com.example.demo.additional"));
                    scanner = new CodeBaseScanner(config);
                }

                @Test
                @DisplayName("it finds correct number of methods")
                public void scanFilterAdditionalPackage() throws IOException {
                    List<Method> actual = scanner.scan().getMethods();
                    assertThat(actual).hasSize(19);
                }
            }

        }

        @Nested
        @DisplayName("if getter and setter is filtered")
        class FilterGetterSetterTest {

            @BeforeEach
            public void setFilter() {
                config.setExcludeGetterSetter(true);
            }

            @Test
            @DisplayName("it finds correct number of methods")
            public void scanFilterGetterSetter() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).hasSize(50);
            }
        }

        @Nested
        @DisplayName("if packages is set to com.example.demo.extmodel")
        class RecursiveTest {

            @BeforeEach
            public void setPackages() {
                config.setPackages(Collections.singletonList("com.example.demo.extmodel"));
            }

            @Test
            @DisplayName("it finds methods successfully")
            public void scanRecursively() throws IOException {
                List<Method> actual = scanner.scan().getMethods();
                assertThat(actual).isNotEmpty();
            }
        }
    }
}
