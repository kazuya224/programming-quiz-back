# ビルドステージ（Java 21に変更）
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# すべてコピー
COPY . .

# 権限付与とビルド
RUN chmod +x ./demo/mvnw
RUN ./demo/mvnw clean install -DskipTests -f demo/pom.xml

# 実行ステージ（Java 21に変更）
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# ビルド結果のパスを合わせる
COPY --from=build /app/demo/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=10000"]