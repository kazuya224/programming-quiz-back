# ビルドステージ
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# まずは demo フォルダの中身をコピーするように指定
COPY . .

# ファイルがあるか確認（デバッグ用：ログに出力されます）
RUN ls -la

# もし demo/mvnw という構造になっているなら、以下のようにパスを合わせます
# ※ Root Directory を demo にしているなら本来は ./mvnw でいいはずですが、
# うまくいかない場合は直に指定します。
RUN chmod +x ./mvnw
RUN ./mvnw clean install -DskipTests

# 実行ステージ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]