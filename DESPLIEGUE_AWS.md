# Despliegue en AWS EC2

Guía para subir el Sistema de Gestión de Insumos a una instancia EC2 y abrirlo con la IP pública.

**Qué se sube:** un solo archivo, `dist/GestionInsumos.jar` (~14 MB). Ya incluye el servidor web, el driver de SQLite y la página. No necesita Tomcat, MySQL ni nada más, solo **Java 17 o superior**.

---

## 1. Generar el .jar

En la carpeta del proyecto, con el JDK instalado:

- Windows: doble clic en `compilar.bat`
- Linux/Mac: `./compilar.sh`

Queda en `dist/GestionInsumos.jar`. (El zip del proyecto ya trae uno compilado).

Probarlo en local: `java -jar dist/GestionInsumos.jar` y abrir http://localhost:8080

## 2. Crear la instancia EC2

En la consola de AWS → EC2 → **Launch instance**:

- **AMI:** Amazon Linux 2023
- **Tipo:** t2.micro o t3.micro (capa gratuita)
- **Key pair:** crear una y guardar el `.pem`
- **Security group**, reglas de entrada:
  | Tipo | Puerto | Origen |
  |---|---|---|
  | SSH | 22 | My IP |
  | HTTP | 80 | 0.0.0.0/0 (Anywhere) |

Recomendado: asignar una **Elastic IP** a la instancia para que la IP no cambie si se apaga y se vuelve a encender.

## 3. Subir el .jar

Desde tu computador (cambia `llave.pem` e `IP-PUBLICA`):

```bash
scp -i llave.pem dist/GestionInsumos.jar ec2-user@IP-PUBLICA:/home/ec2-user/
scp -i llave.pem deploy/gestion-insumos.service ec2-user@IP-PUBLICA:/home/ec2-user/
```

## 4. Instalar y dejar corriendo

Conectarse: `ssh -i llave.pem ec2-user@IP-PUBLICA` y ejecutar:

```bash
# Java 21
sudo dnf install -y java-21-amazon-corretto-headless

# Carpeta de la aplicación
sudo mkdir -p /opt/gestion-insumos/datos
sudo mv ~/GestionInsumos.jar /opt/gestion-insumos/
sudo chown -R ec2-user:ec2-user /opt/gestion-insumos

# Servicio: arranca solo y se reinicia si se cae
sudo mv ~/gestion-insumos.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now gestion-insumos
sudo systemctl status gestion-insumos      # debe decir "active (running)"
```

Abrir en el navegador: **http://IP-PUBLICA**

Usuario inicial: `admin` / `admin123`. **Cambiar esa contraseña apenas se entre** (Mi perfil → Cambiar contraseña), porque la página queda pública en internet.

## 5. Comandos útiles

| Para | Comando |
|---|---|
| Ver los registros (logs) | `sudo journalctl -u gestion-insumos -f` |
| Reiniciar | `sudo systemctl restart gestion-insumos` |
| Detener | `sudo systemctl stop gestion-insumos` |
| Actualizar a una versión nueva | subir el nuevo `.jar` a `/opt/gestion-insumos/` y reiniciar |
| Copia de seguridad de la BD | `cp /opt/gestion-insumos/datos/gestion_insumos.db ~/respaldo.db` |

La base de datos queda en `/opt/gestion-insumos/datos/gestion_insumos.db` y **no se borra** al actualizar el `.jar`.

## 6. Cambiar los estilos (sin recompilar)

La página viene dentro del `.jar`, pero si existe una carpeta `web/` junto a él, se usa esa. Para personalizar el diseño:

```bash
# desde tu computador, sube la carpeta web del proyecto
scp -i llave.pem -r web ec2-user@IP-PUBLICA:/opt/gestion-insumos/
```

Luego se edita `/opt/gestion-insumos/web/css/estilos.css` (o el HTML) y basta con **recargar la página**; no hace falta reiniciar el servicio. Los colores principales están en las variables de `:root` al inicio del CSS.

Si se cambian los `id` o las clases del HTML, revisar que `web/js/app.js` los siga encontrando.

## 7. Variables de configuración

| Variable | Por defecto | Para qué |
|---|---|---|
| `PORT` | 8080 | Puerto del servidor (el servicio usa 80) |
| `DB_PATH` | `gestion_insumos.db` | Ruta del archivo SQLite |
| `WEB_DIR` | `web` | Carpeta con la página (si no existe, usa la interna del .jar) |

La zona horaria está fija en `America/Bogota` dentro del código, para que "hoy" sea el día de Colombia aunque el servidor esté en EE. UU.

## Alternativa: Docker

```bash
./compilar.sh
docker build -t gestion-insumos .
docker run -d -p 80:8080 -v insumos-datos:/app/datos --restart unless-stopped --name insumos gestion-insumos
```

## Nota sobre HTTPS

Con esta guía la página funciona por **HTTP**, así que las contraseñas viajan sin cifrar. Para el proyecto de clase está bien. Para uso real habría que poner un dominio y un proxy con certificado (Nginx o Caddy + Let's Encrypt) delante del puerto 8080.
