--liquibase formatted sql
-- Agrega user_id a la tabla customer para poder encontrar la cuenta con el sub del token,
-- sin estar llamando al security-service en cada peticion.
--
-- Como el token no lleva el person_id (a proposito, el token no lleva datos personales), esta
-- columna es el puente entre el usuario y su perfil de cliente.
--
-- Va en NULL cuando todavia no hay cuenta (un walk-in). Sin FK porque es una referencia a otro
-- esquema, y con indice unico para que un usuario no tenga dos perfiles.
--
-- La otra opcion era meter el person_id en el token, pero el security-service ya dejo claro
-- que el token no lleva datos personales, asi que no va.

--changeset lavarapido:customer-014-add-user-id
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON c.object_id = t.object_id JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'customer' AND t.name = 'customer' AND c.name = 'user_id'
ALTER TABLE [customer].[customer] ADD user_id BIGINT NULL;

--changeset lavarapido:customer-014-ux-customer-user
--comment: Un usuario no puede tener dos filas de customer, y las borradas no cuentan
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON i.object_id = t.object_id JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'customer' AND t.name = 'customer' AND i.name = 'ux_customer_user'
CREATE UNIQUE NONCLUSTERED INDEX ux_customer_user ON [customer].[customer] (user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL;
