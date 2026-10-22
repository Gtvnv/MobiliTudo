# Build em duas etapas: a imagem final leva apenas o JRE e o jar, sem Maven nem fontes.
FROM maven:3.9-eclipse-temurin-21 AS construcao
WORKDIR /construcao

# As dependencias mudam muito menos que o codigo; resolve-las antes aproveita o cache
# de camadas do Docker em toda recompilacao que nao mexe no pom.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Processo sem privilegios: nada aqui precisa de root.
RUN addgroup -S mobilitudo && adduser -S mobilitudo -G mobilitudo
USER mobilitudo

COPY --from=construcao /construcao/target/*.jar aplicacao.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/aplicacao.jar"]
