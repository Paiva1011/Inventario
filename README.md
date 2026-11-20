# Sistema de Inventario

[![CI/CD](https://github.com/Paiva1011/Inventario/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Paiva1011/Inventario/actions/workflows/ci-cd.yml)

Aplicación web para controlar productos, categorías, movimientos de stock (entradas y salidas), reportes y usuarios.

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, MySQL
- **Frontend:** HTML, CSS y JavaScript (en `src/main/resources/static`), servido por el mismo Spring Boot
- **Pruebas:** JUnit 5 y Mockito (servicios y controladores)
- **CI/CD:** GitHub Actions + Docker

## Ejecutar en local

1. Tener **Java 21** y **MySQL** encendido, con la base creada:
   ```sql
   CREATE DATABASE inventario_db;
   ```
2. Crear el archivo `src/main/resources/application-local.properties` (está en `.gitignore`, no se sube a GitHub):
   ```
   spring.datasource.password=TU_CONTRASEÑA_DE_MYSQL
   ```
3. Ejecutar:
   ```
   ./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
   ```
4. Abrir <http://localhost:9090> (documentación de la API: `/swagger-ui/index.html`).

### Variables de entorno (opcionales, tienen prioridad)

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/inventario_db` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(vacío)* |
| `PORT` | `9090` |

## Pruebas

```
./mvnw test
```
`InventarioApplicationTests` necesita MySQL encendido. Para correr solo las unitarias:
`./mvnw test -Dtest="*ServiceTest,*ControllerTest"`

## CI/CD

El flujo está en `.github/workflows/ci-cd.yml`.

| Cuándo | Qué pasa |
|---|---|
| Push o Pull Request a `develop` o `master` | **CI:** compila y corre todas las pruebas con un MySQL temporal |
| Push a `master` (si el CI pasó) | **CD:** genera el JAR, publica la imagen Docker en GitHub Container Registry y dispara el despliegue |
| Tag `vX.Y.Z` | Además crea un **Release** en GitHub con el JAR adjunto |

```
feature/*  ->  PR a develop  ->  PR a master  ->  despliegue automático
              (CI valida)        (CI valida)       (CD publica y despliega)
```

Crear un release:
```
git tag v1.0.0
git push origin v1.0.0
```

### Configurar el despliegue (Render)

1. En <https://render.com> crear un **Web Service** desde este repositorio, con entorno **Docker**.
2. En *Environment*, definir `DB_URL`, `DB_USER` y `DB_PASSWORD` apuntando a un MySQL en la nube
   (por ejemplo `jdbc:mysql://HOST:PUERTO/inventario_db`). Render no ofrece MySQL propio: usar un proveedor externo.
3. En *Settings > Auto-Deploy* poner **Off**, para que solo se despliegue cuando el CI pase.
4. Copiar el **Deploy Hook** del servicio (Settings > Deploy Hook).
5. En GitHub: *Settings > Secrets and variables > Actions > New repository secret*
   - Nombre: `RENDER_DEPLOY_HOOK`
   - Valor: la URL del hook.

Sin ese secreto, el job de despliegue se omite y el resto del pipeline funciona igual.

### Imagen Docker

Cada push a `master` publica `ghcr.io/paiva1011/inventario:latest`. Para probarla en local:
```
docker run -p 9090:9090 -e DB_URL=jdbc:mysql://host.docker.internal:3306/inventario_db -e DB_USER=root -e DB_PASSWORD=xxxx ghcr.io/paiva1011/inventario:latest
```
