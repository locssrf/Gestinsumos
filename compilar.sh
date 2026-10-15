#!/bin/sh
# Compila el proyecto y genera dist/GestionInsumos.jar (incluye SQLite y la pagina web).
# Uso: ./compilar.sh      Luego: java -jar dist/GestionInsumos.jar
set -e
cd "$(dirname "$0")"
rm -rf out dist
mkdir -p out dist

echo "1/3 Compilando..."
javac -encoding UTF-8 --release 17 -d out -cp lib/sqlite-jdbc.jar $(find src -name "*.java")

echo "2/3 Agregando SQLite y la pagina web..."
(cd out && jar xf ../lib/sqlite-jdbc.jar && rm -rf META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA)
cp -r web out/publico

echo "3/3 Creando el .jar..."
jar --create --file dist/GestionInsumos.jar --main-class app.MainWeb -C out .

echo "Listo: dist/GestionInsumos.jar"
echo "Ejecutar con: java -jar dist/GestionInsumos.jar   y abrir http://localhost:8080"
