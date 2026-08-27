# ── Stage 1: Build ──────────────────────────────────────
# Full JDK + the Maven wrapper, used only to compile. None of this reaches the
# final image.
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Dependency manifests first. Docker caches this layer, so as long as pom.xml is
# unchanged a rebuild skips the whole dependency download — which is the slowest
# part of the build by a wide margin.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# chmod is not cosmetic: the executable bit on mvnw depends on how the build
# context was produced. Cloned on Windows, or copied through a filesystem that
# does not carry POSIX permissions, mvnw arrives as 0644 and `./mvnw` fails with
# "permission denied" — a confusing failure given the file is plainly there.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Split the fat jar into Spring Boot's standard layers. Dependencies change
# rarely and application classes change on every commit; keeping them in
# separate image layers means a code-only rebuild re-ships a few hundred KB
# instead of the whole ~60 MB artifact.
# `-Djarmode=tools` is the supported form as of Spring Boot 3.3; the older
# `-Djarmode=layertools` still works but prints a deprecation warning on every
# build. --layers reproduces the same four layer directories, --launcher extracts
# the loader classes so JarLauncher can start the exploded app below.
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ── Stage 2: Run ─────────────────────────────────────────
# JRE only — no compiler, no Maven, no source.
FROM eclipse-temurin:21-jre-alpine

# Create the unprivileged user before copying, so --chown can reference it and
# the application files are never owned by root.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Ordered least- to most-frequently-changed, which is what makes the layering
# above pay off.
COPY --from=build --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/application/ ./

USER spring

# The JVM reads the container's memory limit, but its default heap ceiling of
# 25% of it leaves most of a small container unused. 75% is the usual figure for
# a single-process container.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8082

# /actuator/health is permitAll in SecurityConfig, so this needs no credentials.
# start-period covers JVM + Hibernate + Flyway startup so a slow boot is not
# counted as a failure. Compose gates the frontend on this (service_healthy),
# which is what stops nginx answering 502 while Spring is still coming up.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"' || exit 1

# JarLauncher rather than `java -jar`: the jar was expanded into layers above, so
# there is no longer a single archive to hand to -jar.
# NB: the package moved to ...loader.launch in Spring Boot 3.2+.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
