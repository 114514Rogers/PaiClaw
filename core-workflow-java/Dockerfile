FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9.9-eclipse-temurin-21-noble AS build

LABEL maintainer="paiflow-workflow-java"
LABEL version="1.0.0"

WORKDIR /app

# 设置时区为上海
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制构建好的 JAR 文件
COPY target/workflow-java.jar /app/workflow-java.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 7881

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:7881/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-jar", \
    "/app/workflow-java.jar"]
