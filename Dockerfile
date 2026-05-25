# 1. Base 이미지 설정
FROM amazoncorretto:21-al2023-jdk

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일을 컨테이너 내부로 복사
# egobookServer.jar : Host Server에 있는 파일명
# app.jar: Docker Container 내부에 들어갈 파일명을 지정
# 즉, EC2 Host Server에 있는 파일을 Docker Container 내부로 보낼 때의 파일명을 지정해서 복사하는 것이다.
COPY egobookServer.jar app.jar

# 4. 환경 변수 설정 (Timezone)
ENV TZ=Asia/Seoul

# 5. 해당 이미지의 도커 컨테이너가 실행될 때 실행될 고정적 명령어 (Profiles 설정 포함)
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]