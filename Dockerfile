FROM eclipse-temurin:21-jdk AS builder

WORKDIR /opt/app

COPY pom.xml .
COPY ./pom.xml pom.xml

RUN mvn dependency:go-offline -B

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /opt/app

COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /opt/app/ /opt/app

COPY src /opt/app/src

RUN mvn package -B -DskipTests=true

FROM gcr.io/distroless/java21-debian12

WORKDIR /opt/app

COPY --from=builder /opt/app/target/*.jar serviceapp.jar

ENTRYPOINT ["java", "-jar", "/opt/app/serviceapp.jar"]
