# 使用 Maven 官方镜像构建应用
FROM maven:3.8.6-openjdk-17 AS build

# 将项目源代码复制到容器中
WORKDIR /app
COPY . .

# 使用 Maven 构建项目
RUN mvn clean package

# 使用 OpenJDK 运行时镜像运行应用
FROM openjdk:17-jre-slim

# 将构建好的 JAR 文件复制到运行时镜像中
COPY --from=build /app/target/my-app.jar /app/my-app.jar

# 设置容器启动时运行的命令
ENTRYPOINT ["java", "-jar", "/app/wflow-server.jar"]

# 暴露应用运行的端口
EXPOSE 10000