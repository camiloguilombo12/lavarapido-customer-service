# customer-service

Perfil de cliente y sus vehículos. Es el dueño del esquema `customer` y de sus tres tablas
(`vehicle_type`, `customer`, `customer_vehicle`).

- **Puerto:** 3002
- **Paquete raíz:** `com.lavarapido.customer`
- **Java:** 21 · **Spring Boot:** 4.0.8
- **Base de datos:** SQL Server (una instancia, un esquema por servicio — ADR-009)

## Endpoints

Todos bajo `/api/v1`. Salvo el catálogo de tipos de vehículo, todos exigen un token válido
emitido por `security-service`. Las rutas de "mis vehículos" son solo del rol `CLIENT`: un admin o
un operario recibe `403` y no se le crea perfil de cliente (BIZ-12).

| Método  | Ruta                          | Acceso  | Qué hace                                        |
|---------|-------------------------------|---------|-------------------------------------------------|
| `GET`   | `/api/v1/vehicles`            | Cliente | Lista los vehículos del cliente que llama        |
| `GET`   | `/api/v1/vehicles/{id}`       | Cliente | Un vehículo propio (ajeno: `404`). booking-service lo usa con el token del cliente para validar la propiedad |
| `POST`  | `/api/v1/vehicles`            | Cliente | Registra un vehículo                             |
| `PUT`   | `/api/v1/vehicles/{id}`       | Cliente | Edita un vehículo propio                         |
| `DELETE`| `/api/v1/vehicles/{id}`       | Cliente | Retira un vehículo (borrado lógico) y responde `204`; `409 VEHICLE_HAS_ACTIVE_BOOKINGS` si tiene reservas activas, `503 BOOKING_SERVICE_UNAVAILABLE` si booking no responde (BIZ-08) |
| `GET`   | `/api/v1/admin/vehicles?ids=1,2,3` | Admin y operario | Vehículos de cualquier cliente con su dueño (`ownerUserId`); hasta 100 ids. booking-service lo usa en las reservas del admin y del operario |
| `GET`   | `/api/v1/admin/vehicles?plate=ABC123` | Admin y operario | Busca un vehículo activo por placa (el admin reserva a nombre de un cliente) |
| `GET`   | `/api/v1/vehicle-types`       | Público | Catálogo de tipos de vehículo                    |
| `GET`   | `/actuator/health`            | Público | Sonda de salud                                   |

El id del cliente **nunca** se recibe en la petición: sale del claim `sub` del token. Un cliente
que invente un id en la ruta no alcanza a tocar el vehículo de otro, porque el dominio lo busca
dentro de su propia cuenta y responde `404`.

Un vehículo ajeno también es `404` y no `403`: un `403` confirmaría que ese vehículo existe, y eso
ya es información de otro cliente.

### Ejemplo

```bash
# Requiere un token del security-service
curl -H "Authorization: Bearer $TOKEN" http://localhost:3002/api/v1/vehicles

curl -X POST http://localhost:3002/api/v1/vehicles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"licensePlate":"abc-123","vehicleType":"CAR","brand":"Mazda","model":"CX-5","color":"Rojo"}'
```

## Reglas de negocio

**Formato de la placa.** Depende del tipo de vehículo, según las placas colombianas: carro,
camioneta o camión es `ABC123` (3 letras, 3 números); moto es `ABC12D` (3 letras, 2 números,
1 letra). Vive en `LicensePlatePolicy`, no en la base ni en el frontend.

**Normalización.** La placa se guarda siempre en mayúsculas y sin separadores, aunque el usuario
escriba `abc-123`. Eso es lo que hace que `ux_customer_vehicle_plate_active` sirva de algo: sin
normalizar, la misma placa escrita de dos formas sería única dos veces. Una placa con más de 6
letras y números se rechaza con `INVALID_PLATE` al crear, editar o buscar (antes se cortaba y podía
quedar la placa de otro vehículo, E-16). Una placa más larga que ya estaba guardada (la columna
admite 10) se sigue leyendo: el límite se exige cuando alguien la vuelve a escribir.

**Unicidad de la placa.** Es entre **todos** los clientes, no dentro de una cuenta. La comprueba el
dominio antes de escribir y la garantiza el índice único filtrado de la base. El borrado lógico es
lo que permite volver a registrar la misma placa después.

**Borrado lógico.** Nunca se borra una fila. Un vehículo borrado es indistinguible de uno que
nunca existió, para todo lo que mira la API.

**Retirar con reservas activas (BIZ-08).** Antes de retirar un vehículo se le pregunta a
booking-service `GET /internal/v1/bookings/active-count?customerVehicleId={id}` con la llave
`X-Internal-Key`. Si tiene reservas en curso, o programadas/confirmadas que no han terminado, es
`409 VEHICLE_HAS_ACTIVE_BOOKINGS` (si no, quedarían huérfanas ocupando la bahía). Si booking no
responde, o falta `INTERNAL_API_KEY`, es `503 BOOKING_SERVICE_UNAVAILABLE`: no se retira a ciegas.

**Puntos de fidelización.** `customer.loyalty_points` es una **copia de display**: el saldo real
es el libro mayor `payment.loyalty_transaction`. Por eso este servicio no calcula nada, solo
guarda el número que le pasa otro servicio.

## Decisiones de esquema

El DDL maestro (`lavarapido-6-services-sqlserver.sql`) no tenía tres cosas que este servicio
necesita. Se agregan como changesets aparte, con su número en la numeración global del proyecto:

| Changeset | Qué hace                                                                 | Por qué                                                                 |
|-----------|--------------------------------------------------------------------------|------------------------------------------------------------------------|
| `014`     | `customer.user_id BIGINT NULL` + índice único filtrado                   | Permite encontrar la cuenta con el claim `sub` del token, sin llamar al `security-service` en cada petición. Queda `NULL` para el cliente walk-in sin cuenta. |
| `015`     | `customer_vehicle.model NVARCHAR(50) NULL`                               | El formulario de vehículo del frontend pide marca, **modelo**, placa y color; la tabla solo tenía marca y color. |
| `102`     | Seed de los 6 tipos de vehículo                                          | Catálogo que el frontend ya consumía de forma fija. |

El patrón sigue el del `security-service`: triggers `tr_touch_` por tabla, cada uno con su
changeset, en vez de un generador global.

### `size_factor`: supuesto de negocio, no un dato

El DDL dice que `size_factor` **solo sugiere** un precio cuando un administrador crea una tarifa.
No participa en ningún cobro: el precio sale siempre de `catalog.service_price`. Los valores
sembrados (moto `0.80`, carro `1.00`, camioneta SUV `1.15`, camioneta `1.20`, camión `1.35`) son
**relativos y están sin confirmar con el cliente** — no salen del código ni de la documentación.
Un administrador puede cambiarlos sin que ningún cobro se altere, precisamente porque nada se
calcula con esa columna.

## Cómo se crea la fila de cliente

`customer.customer` requiere `person_id`, que vive en el esquema de seguridad y que el token
**no** lleva (el token está declarado como "sin datos personales", ADR-006). Hay dos caminos:

1. **Por evento.** Con `MESSAGING_ENABLED=true`, `UserRegisteredListener` escucha
   `security.user_registered` (cola `customer-service.events`, con su DLQ) y crea el perfil solo si
   la cuenta tiene el rol `CLIENT`. Es idempotente: los mensajeros entregan al menos una vez.
2. **Al primer uso.** Si el evento no llegó (RabbitMQ apagado o cuentas viejas), la primera vez que
   el cliente toca "mis vehículos" se le pide a security-service `GET /users/me` con su mismo token
   y se crea el perfil con ese `person_id`. Si ni así se puede, `409 CUSTOMER_NOT_PROVISIONED`: la
   cuenta sí existe, lo que falta es su perfil.

Los eventos que publica este servicio (`CustomerProvisioned`, `VehicleRegistered`, `VehicleUpdated`,
`VehicleRemoved`) hoy solo van al log (`LoggingDomainEventPublisher`): nadie los consume todavía.

## Llamadas a otros servicios

| A quién | Ruta | Con qué | Si no responde |
|---|---|---|---|
| security-service | `GET {SECURITY_SERVICE_URL}/users/me` | el token del cliente | `409 CUSTOMER_NOT_PROVISIONED` |
| booking-service | `GET {BOOKING_SERVICE_URL}/internal/v1/bookings/active-count?customerVehicleId=` | `X-Internal-Key` (`INTERNAL_API_KEY`) | `503 BOOKING_SERVICE_UNAVAILABLE`, no se retira el vehículo |

Ambas con tiempo máximo de 3 s.

## Estructura

Arquitectura hexagonal. El dominio no depende de Spring.

```
com.lavarapido.customer
├── domain/                        sin dependencias de Spring ni de JPA
│   ├── model/                     LicensePlate, CustomerVehicle, CustomerAccount, VehicleType...
│   ├── service/                   LicensePlatePolicy
│   ├── event/                     hechos de dominio, solo con identificadores
│   ├── exception/                 DomainException y sus especializaciones
│   └── port/{in,out}/             los contratos que usan los adaptadores
├── application/usecase/           casos de uso; orquestan el dominio
└── infrastructure/
    ├── adapter/in/web/            controladores, DTOs, ApiExceptionHandler
    ├── adapter/in/messaging/      listener de security.user_registered
    ├── adapter/out/persistence/   entidades JPA y los adaptadores que traducen
    ├── adapter/out/messaging/     publicador de eventos
    ├── adapter/out/identity/      cliente REST de security-service
    ├── adapter/out/booking/       cliente REST de booking-service (reservas activas, BIZ-08)
    └── config/                    seguridad, JWT, CORS, correlation id, beans de dominio
```

**Errores.** Todo sale como `application/problem+json` (RFC 9457) con un `code` estable que el
frontend traduce. `INVALID_PLATE`, `PLATE_ALREADY_REGISTERED`, `VEHICLE_NOT_FOUND`,
`CUSTOMER_NOT_PROVISIONED`, `INVALID_VEHICLE_TYPE`, `VEHICLE_HAS_ACTIVE_BOOKINGS`,
`BOOKING_SERVICE_UNAVAILABLE`, `VALIDATION_ERROR`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`.

## Correrlo

```bash
# 1. Secretos. El .env vive en lavarapido-infra y no se sube.
cp ../lavarapido-infra/.env.example ../lavarapido-infra/.env
#    Llenar DB_PASSWORD y JWT_SECRET (mínimo 32 bytes):
#    openssl rand -base64 48

# 2. Levantar SQL Server (dejarlo arriba)
docker compose -f ../lavarapido-infra/docker-compose.yml up -d sqlserver

# 3. Correr
./mvnw spring-boot:run
```

Liquibase crea el esquema y aplica los changesets al arrancar. `ddl-auto: validate`: Hibernate
solo verifica que el mapeo coincida con la base, nunca la modifica.

### Pruebas

```bash
./mvnw test        # 82 pruebas (24-10-2026), sin base de datos: dominio, política, listener, clientes REST y API web
```

## Variables de entorno

| Variable              | Para qué                                              |
|-----------------------|-------------------------------------------------------|
| `DB_URL`              | JDBC. Por defecto `localhost:1433`, base `LavaRapido` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales                                        |
| `JWT_SECRET`          | Llave HS256 compartida, mínimo 32 bytes (ADR-006)     |
| `JWT_ISSUER`          | `lavarapido-security-service`                         |
| `JWT_AUDIENCE`        | `lavarapido-api`                                      |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:4200,http://localhost:8081`         |
| `PORT`                | `3002`                                                |
| `MESSAGING_ENABLED`   | `true` escucha `security.user_registered` en RabbitMQ (por defecto `false`) |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Conexión a RabbitMQ |
| `SECURITY_SERVICE_URL`| Base de security-service con `/api/v1` (por defecto `http://localhost:3001/api/v1`) |
| `SECURITY_SERVICE_TIMEOUT` | Tiempo máximo de esa llamada (`3s`)              |
| `BOOKING_SERVICE_URL` | **Raíz** de booking-service, sin ruta (por defecto `http://localhost:3003`; en docker `http://booking-service:3003`). Propiedad `app.booking-service.base-url` |
| `BOOKING_SERVICE_TIMEOUT` | Tiempo máximo de esa llamada (`3s`)               |
| `INTERNAL_API_KEY`    | Llave `X-Internal-Key` para `/internal/**` de booking (la misma de los demás servicios). Vacía: retirar un vehículo responde `503`. Propiedad `app.internal.api-key` |

Este servicio **no emite tokens**; solo los verifica. No tiene codificador de contraseñas.

## Proyecto LavaRápido

Este repositorio es parte de LavaRápido (Vehicle Washing), proyecto formativo del Tecnólogo en
Análisis y Desarrollo de Software (ADSO), ficha 3145556, SENA Regional Huila.

| Recurso | Enlace |
|---|---|
| Documentación y manuales (ES/EN, MD · PDF · Word) | [lavarapido-docs](https://github.com/BrayanCABRERA123/lavarapido-docs#readme) |
| Manual técnico | [09_MANUALES/ES/MD/manual-tecnico.md](https://github.com/BrayanCABRERA123/lavarapido-docs/blob/main/09_MANUALES/ES/MD/manual-tecnico.md) |
| Manual de instalación | [09_MANUALES/ES/MD/manual-de-instalacion.md](https://github.com/BrayanCABRERA123/lavarapido-docs/blob/main/09_MANUALES/ES/MD/manual-de-instalacion.md) |
| Levantar todo el sistema | [lavarapido-infra](https://github.com/BrayanCABRERA123/lavarapido-infra) (`docker compose --profile app up -d`) |
| Aplicación desplegada | https://lavarapidovehicular.northcentralus.cloudapp.azure.com |
| Despliegue continuo (CD) | [docs/despliegue-continuo.md](docs/despliegue-continuo.md) |

**Ramas y commits:** se trabaja en `develop` con ramas `feature/`, `fix/` y `docs/` que se
fusionan con `git merge --no-ff`; los commits siguen Conventional Commits
(`feat(scope): ...`, `fix(scope): ...`). Detalle en la
[guía de git del equipo](https://github.com/BrayanCABRERA123/lavarapido-docs#guía-de-git-del-equipo).

**Equipo:** Brayan Estiven Patiño Cabrera, Juan Camilo Guilombo Osorio y Merari Urbano Quintero.
