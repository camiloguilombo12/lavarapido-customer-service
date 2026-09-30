--liquibase formatted sql
-- El formulario del frontend pide marca, modelo, placa y color, pero la tabla solo tenia
-- brand y color. Esta es la que agrega model.
--
-- Va en NULL a proposito: hay carros de segunda mano donde el dueño no sabe el modelo, y no
-- por eso se debe bloquear el registro del vehiculo.

--changeset lavarapido:customer-015-add-model
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON c.object_id = t.object_id JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'customer' AND t.name = 'customer_vehicle' AND c.name = 'model'
ALTER TABLE [customer].customer_vehicle ADD model NVARCHAR(50) NULL;
