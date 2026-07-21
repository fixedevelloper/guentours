# Image de runtime Java 21 basée sur Alpine (très légère ~150 Mo)
FROM eclipse-temurin:21-jre-alpine

# Répertoire de travail dans le conteneur
WORKDIR /app

# Copier le fichier JAR généré par Maven
COPY target/*.jar app.jar

# Exposer le port de l'application
EXPOSE 9080

# Exécuter l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]