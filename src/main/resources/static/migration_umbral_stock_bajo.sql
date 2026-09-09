-- Umbral configurable de stock bajo (ConfiguracionNegocio) para el digest diario de admin
-- (StockBajoScheduler, 7 AM). NULL = usa el default de 5 unidades (ConfiguracionNegocio.UMBRAL_DEFAULT_STOCK_BAJO).

ALTER TABLE configuracion_negocio
    ADD COLUMN umbral_stock_bajo INT NULL;
