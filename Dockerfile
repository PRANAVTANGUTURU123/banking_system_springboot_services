# One Dockerfile builds any service:  docker build --build-arg SERVICE=AccountService .
# (docker-compose.yml does this for all nine.) No local JDK or Maven needed.

# ---- 1. shared libraries (cached once, reused by every service build) ----
FROM maven:3.9-eclipse-temurin-17 AS commons
WORKDIR /build
ENV MAVEN_OPTS="-Xmx512m"
COPY commons-dto commons-dto
COPY commons-security commons-security
COPY commons-observability commons-observability
RUN mvn -B -q -DskipTests install -f commons-dto/pom.xml \
 && mvn -B -q -DskipTests install -f commons-security/pom.xml \
 && mvn -B -q -DskipTests install -f commons-observability/pom.xml

# ---- 2. the service ----
FROM commons AS build
ARG SERVICE
COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
# Download dependencies in their own layer so source-only changes rebuild fast.
# Best effort: anything missed here is fetched by the package step below.
RUN mvn -B -q -f ${SERVICE}/pom.xml dependency:go-offline > /dev/null 2>&1 || true
COPY ${SERVICE}/src ${SERVICE}/src
RUN mvn -B -q -DskipTests -f ${SERVICE}/pom.xml package \
 && cp ${SERVICE}/target/*.jar /build/app.jar

# ---- 3. runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/app.jar app.jar
# Nine JVMs share one Docker VM, so keep each one small; fast startup over peak throughput.
ENV JAVA_TOOL_OPTIONS="-Xmx192m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]
