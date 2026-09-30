--liquibase formatted sql
-- El formulario del frontend pide marca, modelo, placa y color, pero la tabla solo tenia
-- brand y color. Esta es la que agrega model.
--
-- Va en NULL a proposito: hay carros de segunda mano donde el dueño no sabe el modelo, y no
-- por eso se debe bloquear el registro del vehiculo.

--changeset lavarapido:customer-015-add-model
--preconditions onFail:MARK_RAN
--precondition-column-exists table:[customer].customer_vehicle column:model
ALTER TABLE [customer].customer_vehicle ADD model NVARCHAR(50) NULL;
