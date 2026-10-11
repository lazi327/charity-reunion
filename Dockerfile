# 1) 前端:构建 Vue dist
FROM node:22-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

# 2) 后端:把 dist 作为静态资源一起打进可执行 jar,单容器单域名
FROM maven:3.9-eclipse-temurin-17 AS server
WORKDIR /src
COPY server/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY server/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# 3) 运行时:Render 注入 PORT,上传目录固定在 /data/upload
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=server /src/target/reunion-server-1.0.0.jar app.jar
# 演示轮播图随镜像分发,启动时补种进上传目录(-n 不覆盖用户已上传的文件)
COPY server/upload/ /opt/demo-upload/
ENV PORT=10000
ENV UPLOAD_DIR=/data/upload
EXPOSE 10000
# 免费档只有 256MB,按容器内存上限给堆,避免被 OOMKill
CMD ["sh", "-c", "mkdir -p \"$UPLOAD_DIR\" && cp -rn /opt/demo-upload/. \"$UPLOAD_DIR\"/ 2>/dev/null; java -XX:MaxRAMPercentage=60 -XX:+UseSerialGC -jar /app/app.jar"]
