# Alternativa con Docker (opcional). Antes de construir: ./compilar.sh
#   docker build -t gestion-insumos .
#   docker run -d -p 80:8080 -v insumos-datos:/app/datos --name insumos gestion-insumos
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY dist/GestionInsumos.jar .
ENV PORT=8080 \
    DB_PATH=/app/datos/gestion_insumos.db
VOLUME /app/datos
EXPOSE 8080
CMD ["java", "-Xmx256m", "-jar", "GestionInsumos.jar"]
