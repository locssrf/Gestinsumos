@echo off
REM Compila el proyecto y genera dist\GestionInsumos.jar (incluye SQLite y la pagina web).
REM Uso: doble clic o "compilar.bat". Luego: java -jar dist\GestionInsumos.jar
cd /d "%~dp0"
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist

echo 1/3 Compilando...
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 --release 17 -d out -cp lib\sqlite-jdbc.jar @fuentes.txt || goto error
del fuentes.txt

echo 2/3 Agregando SQLite y la pagina web...
pushd out
jar xf ..\lib\sqlite-jdbc.jar || (popd & goto error)
if exist META-INF\MANIFEST.MF del META-INF\MANIFEST.MF
popd
xcopy web out\publico\ /e /i /q > nul

echo 3/3 Creando el .jar...
jar --create --file dist\GestionInsumos.jar --main-class app.MainWeb -C out . || goto error

echo.
echo Listo: dist\GestionInsumos.jar
echo Ejecutar con: java -jar dist\GestionInsumos.jar   y abrir http://localhost:8080
pause
exit /b 0

:error
echo Hubo un error al compilar. Revise que tenga instalado el JDK 17 o superior.
pause
exit /b 1
