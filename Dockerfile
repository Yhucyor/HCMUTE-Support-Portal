# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# (Tùy chọn) Copy thư mục .mvn và mvnw nếu bạn dùng Maven Wrapper
# COPY .mvn .mvn
# COPY mvnw .
# Tải trước các dependencies để cache (giúp build nhanh hơn ở các lần sau)
RUN mvn dependency:go-offline -B

COPY src ./src
# Build ra file .jar
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Mở port 8080 cho ứng dụng
EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
