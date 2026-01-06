FROM eclipse-temurin:24-jdk AS build
WORKDIR /build

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /build/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
