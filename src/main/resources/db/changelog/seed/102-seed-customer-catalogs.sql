--liquibase formatted sql
-- Los 6 tipos de vehiculo. Son los mismos codes que tiene el modal de registro del frontend
-- (shared/dialogs/register-vehicle-modal/vehicle.model.ts).
--
-- OJO con size_factor: esto NO calcula ningun cobro, solo es la sugerencia que ve el admin
-- cuando crea una tarifa nueva. El precio de verdad sale siempre de catalog.service_price.
-- Los numeros de abajo los puse yo como supuesto (moto mas barata que camion), no salen de
-- ningun documento, asi que hay que confirmarlos con el cliente.
--
-- Los id van fijos a proposito para que el catalogo sea el mismo en cualquier instalacion.
-- Aparte de eso no hay nada que dependa de estos numeros.

--changeset lavarapido:customer-102-seed-vehicle-type
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM [customer].vehicle_type
SET IDENTITY_INSERT [customer].vehicle_type ON;
INSERT INTO [customer].vehicle_type (vehicle_type_id, code, name, size_factor, display_order)
SELECT v.vehicle_type_id, v.code, v.name, v.size_factor, v.display_order
FROM (VALUES
    (1, N'CAR',    N'Automóvil',    CAST(1.00 AS DECIMAL(4,2)), 1),
    (2, N'SEDAN',  N'Sedán',        CAST(1.00 AS DECIMAL(4,2)), 2),
    (3, N'SUV',    N'Camioneta SUV',CAST(1.15 AS DECIMAL(4,2)), 3),
    (4, N'PICKUP', N'Camioneta',    CAST(1.20 AS DECIMAL(4,2)), 4),
    (5, N'TRUCK',  N'Camión',       CAST(1.35 AS DECIMAL(4,2)), 5),
    (6, N'MOTO',   N'Motocicleta',  CAST(0.80 AS DECIMAL(4,2)), 6)
) AS v(vehicle_type_id, code, name, size_factor, display_order)
WHERE NOT EXISTS (SELECT 1 FROM [customer].vehicle_type t WHERE t.code = v.code);
SET IDENTITY_INSERT [customer].vehicle_type OFF;
