# Backend Java para un proveedor con contenedores (p. ej. Render).
# Build desde la raíz del repositorio usando: docker build -f backend/Dockerfile .
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN cd backend && chmod +x ./mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/backend/target/springfield-archive-api-*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC"
ENV PORT=10000
EXPOSE 10000
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
