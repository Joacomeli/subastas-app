-- ============================================================================
--  Datos de DEMO para desarrollo local. NO ejecutar en un entorno real.
--
--  Ejecutar DESPUÉS de la primera vez que arranca el backend: Hibernate
--  (ddl-auto=update) agrega personas.email/password, subastas.moneda y la tabla
--  medios_pago, que este script necesita.
--
--  Usuarios (solo para pruebas locales):
--    cliente@demo.local  /  Demo1234!   (categoría común, medio de pago verificado)
--    admin@demo.local    /  Admin1234!  (para ADMIN_EMAILS=admin@demo.local)
--
--  Es idempotente: se puede correr más de una vez.
-- ============================================================================

BEGIN;

INSERT INTO paises (numero, nombre, nombreCorto, capital, nacionalidad, idiomas)
SELECT 1, 'Argentina', 'AR', 'Buenos Aires', 'Argentina', 'Español'
WHERE NOT EXISTS (SELECT 1 FROM paises WHERE numero = 1);

-- Tasador/empleado interno (el backend lo usa como verificador y revisor).
INSERT INTO personas (documento, nombre, estado, email)
SELECT 'EMP-0001', 'Tasador Demo', 'activo', 'tasador@subastas.com'
WHERE NOT EXISTS (SELECT 1 FROM personas WHERE email = 'tasador@subastas.com');

INSERT INTO empleados (identificador, cargo)
SELECT identificador, 'Tasador' FROM personas
WHERE email = 'tasador@subastas.com'
  AND NOT EXISTS (SELECT 1 FROM empleados e WHERE e.identificador = personas.identificador);

-- Admin
INSERT INTO personas (documento, nombre, estado, email, password)
SELECT '10000001', 'Admin Demo', 'activo', 'admin@demo.local',
       '$2b$10$QbeP/l1VVUF3LpnKxh4A0eIrhY1GztJRVmcAsLGxBWTtn8MYoUOZC'
WHERE NOT EXISTS (SELECT 1 FROM personas WHERE email = 'admin@demo.local');

-- Cliente de prueba
INSERT INTO personas (documento, nombre, direccion, estado, email, password)
SELECT '20000002', 'Cliente Demo', 'Calle Falsa 123', 'activo', 'cliente@demo.local',
       '$2b$10$R2HL6VdeUwmZWJg0DTNN6uBloaFrdh8t6IheycqBhdENrmrJ5JVZm'
WHERE NOT EXISTS (SELECT 1 FROM personas WHERE email = 'cliente@demo.local');

INSERT INTO clientes (identificador, numeroPais, admitido, categoria, verificador)
SELECT p.identificador, 1, 'si', 'comun',
       (SELECT identificador FROM personas WHERE email = 'tasador@subastas.com')
FROM personas p
WHERE p.email = 'cliente@demo.local'
  AND NOT EXISTS (SELECT 1 FROM clientes c WHERE c.identificador = p.identificador);

INSERT INTO medios_pago (persona_id, tipo, detalle, ultimos_digitos, verificado)
SELECT p.identificador, 'tarjeta', 'Visa demo', '4242', TRUE
FROM personas p
WHERE p.email = 'cliente@demo.local'
  AND NOT EXISTS (SELECT 1 FROM medios_pago m WHERE m.persona_id = p.identificador);

-- Dueño del artículo y subastador
INSERT INTO personas (documento, nombre, estado, email)
SELECT '30000003', 'Dueño Demo', 'activo', 'duenio@demo.local'
WHERE NOT EXISTS (SELECT 1 FROM personas WHERE email = 'duenio@demo.local');

INSERT INTO duenios (identificador, numeroPais, verificacionFinanciera, verificacionJudicial, calificacionRiesgo, verificador)
SELECT p.identificador, 1, 'si', 'si', 1,
       (SELECT identificador FROM personas WHERE email = 'tasador@subastas.com')
FROM personas p
WHERE p.email = 'duenio@demo.local'
  AND NOT EXISTS (SELECT 1 FROM duenios d WHERE d.identificador = p.identificador);

INSERT INTO personas (documento, nombre, estado, email)
SELECT '40000004', 'Subastador Demo', 'activo', 'subastador@demo.local'
WHERE NOT EXISTS (SELECT 1 FROM personas WHERE email = 'subastador@demo.local');

INSERT INTO subastadores (identificador, matricula, region)
SELECT p.identificador, 'MAT-0001', 'CABA'
FROM personas p
WHERE p.email = 'subastador@demo.local'
  AND NOT EXISTS (SELECT 1 FROM subastadores s WHERE s.identificador = p.identificador);

-- Subasta abierta (la regla del esquema exige fecha > hoy + 10 días), producto, catálogo e ítem.
-- Solo se crea si todavía no hay una subasta de demo.
DO $$
DECLARE
    v_tasador    BIGINT := (SELECT identificador FROM personas WHERE email = 'tasador@subastas.com');
    v_duenio     BIGINT := (SELECT identificador FROM personas WHERE email = 'duenio@demo.local');
    v_subastador BIGINT := (SELECT identificador FROM personas WHERE email = 'subastador@demo.local');
    v_subasta    BIGINT;
    v_producto   BIGINT;
    v_catalogo   BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM subastas WHERE ubicacion = 'Sala Demo (seed)') THEN
        RETURN;
    END IF;

    INSERT INTO subastas (fecha, hora, estado, subastador, ubicacion, capacidadAsistentes,
                          tieneDeposito, seguridadPropia, categoria, moneda)
    VALUES (CURRENT_DATE + 11, '18:00', 'abierta', v_subastador, 'Sala Demo (seed)', 50,
            'si', 'si', 'comun', 'ARS')
    RETURNING identificador INTO v_subasta;

    INSERT INTO productos (fecha, disponible, descripcionCatalogo, descripcionCompleta, revisor, duenio)
    VALUES (CURRENT_DATE, 'si', 'Reloj de bolsillo antiguo',
            'Reloj de bolsillo de plata, circa 1910, en funcionamiento.', v_tasador, v_duenio)
    RETURNING identificador INTO v_producto;

    INSERT INTO catalogos (descripcion, subasta, responsable)
    VALUES ('Catálogo demo', v_subasta, v_tasador)
    RETURNING identificador INTO v_catalogo;

    INSERT INTO itemsCatalogo (catalogo, producto, precioBase, comision, subastado)
    VALUES (v_catalogo, v_producto, 15000.00, 1500.00, 'no');
END $$;

COMMIT;
