# customer-service

Perfil de cliente y sus vehículos. Es el dueño del esquema `customer` y de sus tres tablas
(`vehicle_type`, `customer`, `customer_vehicle`).

- **Puerto:** 3002
- **Paquete raíz:** `com.lavarapido.customer`
- **Java:** 21 · **Spring Boot:** 4.0.8
- **Base de datos:** SQL Server (una instancia, un esquema por servicio — ADR-009)

## Endpoints

Todos bajo `/api/v1`. Salvo el catálogo de tipos de vehículo, todos exigen un token válido
emitido por `security-service`.

| Método  | Ruta                          | Acceso  | Qué hace                                        |
|---------|-------------------------------|---------|-------------------------------------------------|
| `GET`   | `/api/v1/vehicles`            | Cliente | Lista los vehículos del cliente que llama        |
| `POST`  | `/api/v1/vehicles`            | Cliente | Registra un vehículo                             |
| `PUT`   | `/api/v1/vehicles/{id}`       | Cliente | Edita un vehículo propio                         |
| `DELETE`| `/api/v1/vehicles/{id}`       | Cliente | Borra un vehículo (lógico) y responde `204`      |
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
normalizar, la misma placa escrita de dos formas sería única dos veces.

**Unicidad de la placa.** Es entre **todos** los clientes, no dentro de una cuenta. La comprueba el
dominio antes de escribir y la garantiza el índice único filtrado de la base. El borrado lógico es
lo que permite volver a registrar la misma placa después.

**Borrado lógico.** Nunca se borra una fila. Un vehículo borrado es indistinguible de uno que
nunca existió, para todo lo que mira la API.

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
**no** lleva (el token está declarado como "sin datos personales", ADR-006). Por eso la fila no se
crea en el primer endpoint que toca el cliente, sino al consumir el evento
`security.user.registered`, que sí lo trae. Ese camino ya estaba previsto: el comentario del propio
evento en el `security-service` dice que este servicio reacciona creando el perfil de cliente.

`ConsumeUserRegisteredUseCase` está implementado y es idempotente —los mensajeros entregan al
menos una vez—. Cuando todavía no ha llegado el evento, los endpoints responden `409` con
`CUSTOMER_NOT_PROVISIONED`: la cuenta sí existe, lo que falta es su perfil.

**Pendiente:** el adaptador que escucha el broker. Hoy el publicador solo escribe al log
(`LoggingDomainEventPublisher`); cuando se conecte, se reemplaza ese adaptador y ni el dominio ni
los casos de uso cambian.

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
    ├── adapter/out/persistence/   entidades JPA y los adaptadores que traducen
    ├── adapter/out/messaging/     publicador de eventos
    └── config/                    seguridad, JWT, CORS, correlation id, beans de dominio
```

**Errores.** Todo sale como `application/problem+json` (RFC 9457) con un `code` estable que el
frontend traduce. `INVALID_PLATE`, `PLATE_ALREADY_REGISTERED`, `VEHICLE_NOT_FOUND`,
`CUSTOMER_NOT_PROVISIONED`, `INVALID_VEHICLE_TYPE`, `VALIDATION_ERROR`.

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
./mvnw test        # 51 pruebas, sin base de datos: son de dominio y de política
```

## Variables de entorno

| Variable              | Para qué                                              |
|-----------------------|-------------------------------------------------------|
| `DB_URL`              | JDBC. Por defecto `localhost:1433`, base `LavaRapido` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales                                        |
| `JWT_SECRET`          | Llave HS256 compartida, mínimo 32 bytes (ADR-006)     |
| `JWT_ISSUER`          | `lavarapido-security-service`                         |
| `JWT_AUDIENCE`        | `lavarapido-api`                                      |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:4200`                               |
| `PORT`                | `3002`                                                |

Este servicio **no emite tokens**; solo los verifica. No tiene codificador de contraseñas.
