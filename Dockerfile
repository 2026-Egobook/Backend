# 1. Base 이미지 설정
FROM amazoncorretto:21-al2023-jdk

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일을 컨테이너 내부로 복사
# GitHub Actions에서 빌드 후 이름을 egobookServer.jar로 고정하여 전달한다고 가정
COPY egobookServer.jar app.jar

# 4. 환경 변수 설정 (Timezone)
ENV TZ=Asia/Seoul

# 5. 실행 명령어 (Profiles 설정 포함)
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]