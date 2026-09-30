--liquibase formatted sql
-- Las 3 tablas que maneja este servicio: vehicle_type, customer y customer_vehicle.
-- Salen del DDL general del proyecto (lavarapido-6-services-sqlserver.sql, seccion 2).

--changeset lavarapido:customer-004-schema-customer
--comment: Crea el esquema customer, que es de este servicio
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.schemas WHERE name = 'customer'
CREATE SCHEMA [customer];

--changeset lavarapido:customer-004-vehicle-type
--comment: Tipos de vehiculo. Ojo: size_factor es solo una sugerencia para el admin, el precio de verdad sale de catalog.service_price
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'customer' AND t.name = 'vehicle_type'
CREATE TABLE [customer].vehicle_type (
    vehicle_type_id SMALLINT     IDENTITY(1,1) NOT NULL,
    code            NVARCHAR(30) NOT NULL,
    name            NVARCHAR(60) NOT NULL,
    size_factor     DECIMAL(4,2) NOT NULL,
    display_order   SMALLINT     NOT NULL CONSTRAINT df_vtype_order DEFAULT 0,
    is_active       BIT          NOT NULL CONSTRAINT df_vtype_active DEFAULT 1,
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_vtype_created DEFAULT SYSUTCDATETIME(),
    created_by      BIGINT       NULL,
    updated_at      DATETIME2(3) NULL,
    updated_by      BIGINT       NULL,
    deleted_at      DATETIME2(3) NULL,
    deleted_by      BIGINT       NULL,
    row_version     INT          NOT NULL CONSTRAINT df_vtype_rv DEFAULT 1,
    CONSTRAINT pk_vehicle_type PRIMARY KEY (vehicle_type_id),
    CONSTRAINT uq_vehicle_type_code UNIQUE (code),
    CONSTRAINT ck_vehicle_type_size_factor CHECK (size_factor > 0)
);

--changeset lavarapido:customer-004-customer
--comment: La cuenta del cliente. loyalty_points es una copia para mostrar, el saldo real lo lleva payment-service
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'customer' AND t.name = 'customer'
CREATE TABLE [customer].[customer] (
    customer_id    BIGINT       IDENTITY(1,1) NOT NULL,
    person_id      BIGINT       NOT NULL,   -- apunta a security.person, pero sin FK: cada servicio no revisa tablas de otro
    loyalty_points INT          NOT NULL CONSTRAINT df_customer_points DEFAULT 0,
    customer_since DATE         NOT NULL CONSTRAINT df_customer_since DEFAULT CAST(SYSUTCDATETIME() AS DATE),
    created_at     DATETIME2(3) NOT NULL CONSTRAINT df_customer_created DEFAULT SYSUTCDATETIME(),
    created_by     BIGINT       NULL,
    updated_at     DATETIME2(3) NULL,
    updated_by     BIGINT       NULL,
    deleted_at     DATETIME2(3) NULL,
    deleted_by     BIGINT       NULL,
    row_version    INT          NOT NULL CONSTRAINT df_customer_rv DEFAULT 1,
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_person UNIQUE (person_id),
    CONSTRAINT ck_customer_points CHECK (loyalty_points >= 0)
);

--changeset lavarapido:customer-004-customer-vehicle
--comment: Los vehiculos del cliente. La placa solo es unica si el vehiculo no esta borrado, gracias al indice de abajo
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id WHERE s.name = 'customer' AND t.name = 'customer_vehicle'
CREATE TABLE [customer].customer_vehicle (
    customer_vehicle_id BIGINT       IDENTITY(1,1) NOT NULL,
    customer_id         BIGINT       NOT NULL,
    license_plate       NVARCHAR(10) NOT NULL,
    vehicle_type_id     SMALLINT     NOT NULL,
    brand               NVARCHAR(50) NULL,
    color               NVARCHAR(30) NULL,
    created_at          DATETIME2(3) NOT NULL CONSTRAINT df_cveh_created DEFAULT SYSUTCDATETIME(),
    created_by          BIGINT       NULL,
    updated_at          DATETIME2(3) NULL,
    updated_by          BIGINT       NULL,
    deleted_at          DATETIME2(3) NULL,
    deleted_by          BIGINT       NULL,
    row_version         INT          NOT NULL CONSTRAINT df_cveh_rv DEFAULT 1,
    CONSTRAINT pk_customer_vehicle PRIMARY KEY (customer_vehicle_id),
    CONSTRAINT fk_customer_vehicle_customer FOREIGN KEY (customer_id) REFERENCES [customer].[customer](customer_id),
    CONSTRAINT fk_customer_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES [customer].vehicle_type(vehicle_type_id)
);
CREATE UNIQUE NONCLUSTERED INDEX ux_customer_vehicle_plate_active ON [customer].customer_vehicle (license_plate) WHERE deleted_at IS NULL;
CREATE NONCLUSTERED INDEX ix_customer_vehicle_customer ON [customer].customer_vehicle (customer_id);

--changeset lavarapido:customer-004-tr-touch-vehicle-type splitStatements:false
--comment: Trigger que pisa updated_at y row_version. El codigo jamas escribe esas dos columnas
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.triggers WHERE name = 'tr_touch_vehicle_type'
CREATE TRIGGER [customer].tr_touch_vehicle_type
ON [customer].vehicle_type
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;

    UPDATE t
       SET updated_at  = SYSUTCDATETIME(),
           row_version = t.row_version + 1
      FROM [customer].vehicle_type t
      JOIN inserted i ON i.vehicle_type_id = t.vehicle_type_id;
END

--changeset lavarapido:customer-004-tr-touch-customer splitStatements:false
--comment: Trigger que pisa updated_at y row_version. El codigo jamas escribe esas dos columnas
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.triggers WHERE name = 'tr_touch_customer'
CREATE TRIGGER [customer].tr_touch_customer
ON [customer].[customer]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;

    UPDATE c
       SET updated_at  = SYSUTCDATETIME(),
           row_version = c.row_version + 1
      FROM [customer].[customer] c
      JOIN inserted i ON i.customer_id = c.customer_id;
END

--changeset lavarapido:customer-004-tr-touch-customer-vehicle splitStatements:false
--comment: Trigger que pisa updated_at y row_version. El codigo jamas escribe esas dos columnas
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.triggers WHERE name = 'tr_touch_customer_vehicle'
CREATE TRIGGER [customer].tr_touch_customer_vehicle
ON [customer].customer_vehicle
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;

    UPDATE v
       SET updated_at  = SYSUTCDATETIME(),
           row_version = v.row_version + 1
      FROM [customer].customer_vehicle v
      JOIN inserted i ON i.customer_vehicle_id = v.customer_vehicle_id;
END
