FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# copie tout le monorepo (parent + modules)
COPY . .

# build uniquement le module backend et ses dépendances
RUN mvn -pl backend -am -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

# ⚠️ adapte le nom du jar si besoin
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8081
CMD ["sh","-c","java -jar app.jar"]
