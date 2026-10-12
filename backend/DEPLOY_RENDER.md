# Deploy del backend en Render

El backend es Spring Boot (Java 17). Render solo corre la app; la base PostgreSQL es
externa (Render Postgres, Neon, Supabase, etc.) y se configura por variables de entorno.

## Pasos
1. Crear una base PostgreSQL y ejecutar `backend/sql/schema_postgres.sql` sobre ella.
2. En Render: **New → Blueprint** (detecta `render.yaml`) o **New → Web Service** con
   *Root Directory* `backend` y *Runtime* Docker.
3. Cargar las variables de entorno:

| Variable | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | usuario de la base |
| `SPRING_DATASOURCE_PASSWORD` | clave de la base (solo en el dashboard de Render) |
| `JWT_SECRET` | lo genera el Blueprint, o `openssl rand -base64 48` |
| `ADMIN_EMAILS` | emails con permiso para revisar consignaciones |

`PORT` lo inyecta Render. No hay valores por defecto para clave de base ni `JWT_SECRET`:
si faltan, la app no arranca (a propósito).

4. Con Supabase, usar el *Session Pooler* (IPv4) en vez del host directo (IPv6), puerto 5432.
5. Apuntar la app Android: `./gradlew :app:installRelease -PsubastasBaseUrl=https://<tu-app>.onrender.com/`

## Seguridad
- Nunca commitear claves ni archivos `.env`.
- No ejecutar `seed_demo.sql` en producción: crea usuarios con claves de demo.
- `DDL_AUTO` por defecto es `update` para facilitar el desarrollo; en producción usar migraciones o `validate`.
