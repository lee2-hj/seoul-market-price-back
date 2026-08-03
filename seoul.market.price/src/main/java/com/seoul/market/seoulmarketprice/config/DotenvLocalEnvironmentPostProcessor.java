package com.seoul.market.seoulmarketprice.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 프로젝트 루트의 .env.local 파일을 읽어 Spring Environment에 등록한다.
 *
 * <p>
 * Node/Vite 프로젝트에서 흔히 쓰는 .env.local 관례를 그대로 따르기 위한
 * 클래스이다. application.yml의 {@code ${LOGIN_PAGE}}, {@code ${ROOT_PAGE}}
 * 같은 플레이스홀더는 이 클래스가 등록한 값으로 채워진다.
 * </p>
 *
 * <p>
 * IntelliJ의 Run/Debug 설정은 작업 디렉터리(working directory)가
 * {@code gradlew bootRun}을 실행할 때와 다르게 잡히는 경우가 있다.
 * 작업 디렉터리만 믿으면 .env.local을 못 찾을 수 있으므로,
 * 작업 디렉터리와 이 클래스가 실제로 로드된 위치
 * (컴파일된 .class 파일이 있는 build/classes 폴더)
 * 두 곳 모두에서 시작해 상위 폴더로 거슬러 올라가며 찾는다.
 * 어느 경로로 실행하든 .class 파일은 항상 모듈 폴더(.env.local이
 * 있는 위치) 하위에 있으므로, 작업 디렉터리가 어긋나 있어도
 * 이 경로 기준 탐색은 안정적으로 모듈 폴더를 찾아낸다.
 * </p>
 *
 * <p>
 * .env.local은 다른 설정 파일과 마찬가지로 Git에 커밋하지 않는다.
 * 끝까지 못 찾으면 조용히 건너뛴다(운영 환경처럼 실제 OS 환경변수로
 * LOGIN_PAGE/ROOT_PAGE를 주입하는 경우 이 파일이 없어도 정상 동작한다).
 * </p>
 */
public class DotenvLocalEnvironmentPostProcessor
        implements EnvironmentPostProcessor {

    private static final String DOTENV_FILENAME = ".env.local";

    /**
     * 작업 디렉터리부터 상위로 몇 단계까지 .env.local을 찾아볼지이다.
     *
     * 프로젝트 폴더 구조가 예상보다 깊어도 찾을 수 있도록
     * 넉넉하게 잡아둔다.
     */
    private static final int MAX_PARENT_LEVELS = 10;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        Path dotenvPath = findDotenvFile();

        if (dotenvPath == null) {
            return;
        }

        Properties properties = new Properties();

        try (
                InputStream in = new FileInputStream(dotenvPath.toFile());
                InputStreamReader reader =
                        new InputStreamReader(in, StandardCharsets.UTF_8)
        ) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    DOTENV_FILENAME + " 파일을 읽는 중 오류가 발생했습니다.",
                    e
            );
        }

        Map<String, Object> dotenvProperties = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            dotenvProperties.put(
                    key,
                    properties.getProperty(key).trim()
            );
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource("dotenvLocal", dotenvProperties)
        );
    }

    /**
     * 작업 디렉터리와 클래스 로드 위치, 두 시작점에서 각각
     * 상위 폴더로 거슬러 올라가며 .env.local 파일을 찾는다.
     *
     * @return 찾은 .env.local의 경로, 못 찾았으면 null
     */
    private Path findDotenvFile() {

        for (Path start : searchStartDirectories()) {

            Path found = searchUpward(start);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * .env.local 탐색을 시작할 후보 디렉터리 목록을 반환한다.
     *
     * @return 현재 작업 디렉터리, 클래스 로드 위치 순의 목록
     *         (둘 중 확인할 수 없는 값은 제외한다)
     */
    private List<Path> searchStartDirectories() {

        List<Path> startDirectories = new ArrayList<>();

        startDirectories.add(Path.of("").toAbsolutePath());

        Path codeSourceDirectory = codeSourceDirectory();

        if (codeSourceDirectory != null) {
            startDirectories.add(codeSourceDirectory);
        }

        return startDirectories;
    }

    /**
     * 이 클래스가 실제로 로드된 위치(디렉터리)를 반환한다.
     *
     * <p>
     * gradlew bootRun이나 IDE에서 컴파일해서 실행할 때는
     * build/classes/java/main 같은 디렉터리이고,
     * 패키징된 jar로 실행할 때는 jar 파일이 있는 디렉터리이다.
     * </p>
     *
     * @return 클래스 로드 위치의 디렉터리, 확인할 수 없으면 null
     */
    private Path codeSourceDirectory() {

        CodeSource codeSource =
                DotenvLocalEnvironmentPostProcessor.class
                        .getProtectionDomain()
                        .getCodeSource();

        if (codeSource == null) {
            return null;
        }

        try {
            Path location = Path.of(codeSource.getLocation().toURI());

            return Files.isDirectory(location)
                    ? location
                    : location.getParent();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 주어진 디렉터리부터 상위 폴더로 거슬러 올라가며
     * .env.local 파일을 찾는다.
     *
     * @param start 탐색을 시작할 디렉터리
     * @return 찾은 .env.local의 경로, 못 찾았으면 null
     */
    private Path searchUpward(Path start) {

        Path directory = start;

        for (
                int level = 0;
                directory != null && level <= MAX_PARENT_LEVELS;
                level++
        ) {
            Path candidate = directory.resolve(DOTENV_FILENAME);

            if (Files.exists(candidate)) {
                return candidate;
            }

            directory = directory.getParent();
        }

        return null;
    }
}
