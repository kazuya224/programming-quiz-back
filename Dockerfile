# ビルドステージ
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# すべてコピー（demoフォルダごとコピーされます）
COPY . .

# 権限付与とビルド（demo/ を付けてパスを指定）
RUN chmod +x ./demo/mvnw
RUN ./demo/mvnw clean install -DskipTests -f demo/pom.xml

# 実行ステージ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# ビルド結果も demo/target の中にあるのでパスを合わせる
COPY --from=build /app/demo/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]