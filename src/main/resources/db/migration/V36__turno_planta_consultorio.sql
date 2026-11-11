-- V36: Agregar columna planta en tabla turno para trazabilidad de infraestructura y turnos de mostrador
ALTER TABLE turno ADD COLUMN planta VARCHAR(50) NULL;
