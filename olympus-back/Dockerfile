# Étape 1 : Build de l'application (Multi-Stage Build)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (mise en cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler le projet en ignorant les tests
RUN mvn clean package -DskipTests

# Étape 2 : Création de l'image de production (JRE uniquement)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copier le JAR généré depuis l'étape 1 vers l'étape 2
COPY --from=build /app/target/*.jar app.jar

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
