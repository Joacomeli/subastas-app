-- ============================================================================
--  SubastasApp — Esquema mínimo (PostgreSQL)
--  Traducción del modelo académico (originalmente SQL Server / T-SQL).
--
--  Notas de traducción:
--   * int IDENTITY            -> BIGINT GENERATED ALWAYS AS IDENTITY
--     (las PK subrogadas usan BIGINT para coincidir con las entidades JPA Long)
--   * varbinary(max)          -> BYTEA
--   * decimal(18,2)           -> NUMERIC(18,2)
--   * GETDATE() / DATEADD     -> CURRENT_DATE / INTERVAL
--   * GO                      -> (eliminado)
--   * Identificadores con tilde reescritos sin tilde (verificacionFinanciera...)
--   * Correcciones de typos del original: 'incativo'->'inactivo',
--     'carrada'->'cerrada', y el "." tras nroPoliza -> ",".
--
--  Es la estructura MÍNIMA: el backend (Hibernate, ddl-auto=update) puede
--  agregar columnas extra (p. ej. personas.email / personas.password) y otras
--  tablas sin romper este esquema.
--
--  Uso:
--    psql -U postgres -h localhost -d subastasdb -f schema_postgres.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS paises (
    numero        INT          NOT NULL,
    nombre        VARCHAR(250) NOT NULL,
    nombreCorto   VARCHAR(250) NULL,
    capital       VARCHAR(250) NOT NULL,
    nacionalidad  VARCHAR(250) NOT NULL,
    idiomas       VARCHAR(150) NOT NULL,
    CONSTRAINT pk_paises PRIMARY KEY (numero)
);

CREATE TABLE IF NOT EXISTS personas (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    documento     VARCHAR(20)  NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    direccion     VARCHAR(250),
    estado        VARCHAR(15)  CONSTRAINT chkEstado CHECK (estado IN ('activo', 'inactivo')),
    foto          BYTEA,
    CONSTRAINT pk_personas PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS empleados (
    identificador BIGINT       NOT NULL,
    cargo         VARCHAR(100),
    sector        INT          NULL,
    CONSTRAINT pk_empleados PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS sectores (
    identificador      BIGINT GENERATED ALWAYS AS IDENTITY,
    nombreSector       VARCHAR(150) NOT NULL,
    codigoSector       VARCHAR(10)  NULL,
    responsableSector  BIGINT       NULL,
    CONSTRAINT pk_sectores PRIMARY KEY (identificador),
    CONSTRAINT fk_sectores_empleados FOREIGN KEY (responsableSector) REFERENCES empleados (identificador)
);

CREATE TABLE IF NOT EXISTS seguros (
    nroPoliza        VARCHAR(30)   NOT NULL,
    compania         VARCHAR(150)  NOT NULL,
    polizaCombinada  VARCHAR(2)    CONSTRAINT chkPolizaCombinada CHECK (polizaCombinada IN ('si','no')),
    importe          NUMERIC(18,2) NOT NULL CONSTRAINT chkImporte CHECK (importe > 0),
    CONSTRAINT pk_seguro PRIMARY KEY (nroPoliza)
);

CREATE TABLE IF NOT EXISTS clientes (
    identificador  BIGINT NOT NULL,
    numeroPais     INT,
    admitido       VARCHAR(2)  CONSTRAINT chkAdmitido CHECK (admitido IN ('si','no')),
    categoria      VARCHAR(10) CONSTRAINT chkCategoria CHECK (categoria IN ('comun','especial','plata','oro','platino')),
    verificador    BIGINT NOT NULL,
    CONSTRAINT pk_clientes PRIMARY KEY (identificador),
    CONSTRAINT fk_clientes_personas  FOREIGN KEY (identificador) REFERENCES personas (identificador),
    CONSTRAINT fk_clientes_empleados FOREIGN KEY (verificador)   REFERENCES empleados (identificador),
    CONSTRAINT fk_clientes_paises    FOREIGN KEY (numeroPais)    REFERENCES paises (numero)
);

CREATE TABLE IF NOT EXISTS duenios (
    identificador           BIGINT NOT NULL,
    numeroPais              INT,
    verificacionFinanciera  VARCHAR(2) CONSTRAINT chkVF CHECK (verificacionFinanciera IN ('si','no')),
    verificacionJudicial    VARCHAR(2) CONSTRAINT chkVJ CHECK (verificacionJudicial IN ('si','no')),
    calificacionRiesgo      INT        CONSTRAINT chkCR CHECK (calificacionRiesgo IN (1,2,3,4,5,6)),
    verificador             BIGINT NOT NULL,
    CONSTRAINT pk_duenios PRIMARY KEY (identificador),
    CONSTRAINT fk_duenios_personas  FOREIGN KEY (identificador) REFERENCES personas (identificador),
    CONSTRAINT fk_duenios_empleados FOREIGN KEY (verificador)   REFERENCES empleados (identificador)
);

CREATE TABLE IF NOT EXISTS subastadores (
    identificador BIGINT NOT NULL,
    matricula     VARCHAR(15),
    region        VARCHAR(50),
    CONSTRAINT pk_subastadores PRIMARY KEY (identificador),
    CONSTRAINT fk_subastadores_personas FOREIGN KEY (identificador) REFERENCES personas (identificador)
);

CREATE TABLE IF NOT EXISTS subastas (
    identificador        BIGINT GENERATED ALWAYS AS IDENTITY,
    -- Las subastas tienen al menos 10 días de anticipación al momento de crearlas.
    fecha                DATE       CONSTRAINT chkFecha CHECK (fecha > CURRENT_DATE + INTERVAL '10 days'),
    hora                 TIME       NOT NULL,
    estado               VARCHAR(10) CONSTRAINT chkES CHECK (estado IN ('abierta','cerrada')),
    subastador           BIGINT     NULL,
    ubicacion            VARCHAR(350) NULL,
    capacidadAsistentes  INT        NULL,
    tieneDeposito        VARCHAR(2) CONSTRAINT chkTD CHECK (tieneDeposito IN ('si','no')),
    seguridadPropia      VARCHAR(2) CONSTRAINT chkSP CHECK (seguridadPropia IN ('si','no')),
    categoria            VARCHAR(10) CONSTRAINT chkCS CHECK (categoria IN ('comun','especial','plata','oro','platino')),
    CONSTRAINT pk_subastas PRIMARY KEY (identificador),
    CONSTRAINT fk_subastas_subastadores FOREIGN KEY (subastador) REFERENCES subastadores (identificador)
);

CREATE TABLE IF NOT EXISTS productos (
    identificador        BIGINT GENERATED ALWAYS AS IDENTITY,
    fecha                DATE,
    disponible           VARCHAR(2)   CONSTRAINT chkD CHECK (disponible IN ('si','no')),
    descripcionCatalogo  VARCHAR(500) NULL DEFAULT 'No Posee',
    descripcionCompleta  VARCHAR(300) NOT NULL,
    revisor              BIGINT NOT NULL,
    duenio               BIGINT NOT NULL,
    seguro               VARCHAR(30) NULL,
    CONSTRAINT pk_productos PRIMARY KEY (identificador),
    CONSTRAINT fk_productos_empleados FOREIGN KEY (revisor) REFERENCES empleados (identificador),
    CONSTRAINT fk_productos_duenios   FOREIGN KEY (duenio)  REFERENCES duenios (identificador)
);

CREATE TABLE IF NOT EXISTS fotos (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    producto      BIGINT NOT NULL,
    foto          BYTEA  NOT NULL,
    CONSTRAINT pk_fotos PRIMARY KEY (identificador),
    CONSTRAINT fk_fotos_productos FOREIGN KEY (producto) REFERENCES productos (identificador)
);

CREATE TABLE IF NOT EXISTS catalogos (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    descripcion   VARCHAR(250) NOT NULL,
    subasta       BIGINT NULL,
    responsable   BIGINT NOT NULL,
    CONSTRAINT pk_catalogos PRIMARY KEY (identificador),
    CONSTRAINT fk_catalogos_empleados FOREIGN KEY (responsable) REFERENCES empleados (identificador),
    CONSTRAINT fk_catalogos_subastas  FOREIGN KEY (subasta)     REFERENCES subastas (identificador)
);

CREATE TABLE IF NOT EXISTS itemsCatalogo (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    catalogo      BIGINT NOT NULL,
    producto      BIGINT NOT NULL,
    precioBase    NUMERIC(18,2) NOT NULL CONSTRAINT chkPB CHECK (precioBase > 0.01),
    comision      NUMERIC(18,2) NOT NULL CONSTRAINT chkC  CHECK (comision   > 0.01),
    subastado     VARCHAR(2)    CONSTRAINT chkS CHECK (subastado IN ('si','no')),
    CONSTRAINT pk_itemsCatalogo PRIMARY KEY (identificador),
    CONSTRAINT fk_itemsCatalogo_catalogos FOREIGN KEY (catalogo) REFERENCES catalogos (identificador),
    CONSTRAINT fk_itemsCatalogo_productos FOREIGN KEY (producto) REFERENCES productos (identificador)
);

CREATE TABLE IF NOT EXISTS asistentes (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    numeroPostor  INT    NOT NULL,
    cliente       BIGINT NOT NULL,
    subasta       BIGINT NOT NULL,
    CONSTRAINT pk_asistentes PRIMARY KEY (identificador),
    CONSTRAINT fk_asistentes_clientes FOREIGN KEY (cliente) REFERENCES clientes (identificador),
    CONSTRAINT fk_asistentes_subasta  FOREIGN KEY (subasta) REFERENCES subastas (identificador)
);

CREATE TABLE IF NOT EXISTS pujos (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    asistente     BIGINT NOT NULL,
    item          BIGINT NOT NULL,
    importe       NUMERIC(18,2) NOT NULL CONSTRAINT chkI CHECK (importe > 0.01),
    ganador       VARCHAR(2)    CONSTRAINT chkG CHECK (ganador IN ('si','no')) DEFAULT 'no',
    CONSTRAINT pk_pujos PRIMARY KEY (identificador),
    CONSTRAINT fk_pujos_asistentes     FOREIGN KEY (asistente) REFERENCES asistentes (identificador),
    CONSTRAINT fk_pujos_itemsCatalogo  FOREIGN KEY (item)      REFERENCES itemsCatalogo (identificador)
);

CREATE TABLE IF NOT EXISTS registroDeSubasta (
    identificador BIGINT GENERATED ALWAYS AS IDENTITY,
    subasta       BIGINT NOT NULL,
    duenio        BIGINT NOT NULL,
    producto      BIGINT NOT NULL,
    cliente       BIGINT NOT NULL,
    importe       NUMERIC(18,2) NOT NULL CONSTRAINT chkImportePagado CHECK (importe > 0.01),
    comision      NUMERIC(18,2) NOT NULL CONSTRAINT chkComisionPagada CHECK (comision > 0.01),
    CONSTRAINT pk_registroDeSubasta PRIMARY KEY (identificador),
    CONSTRAINT fk_registroDeSubasta_subastas FOREIGN KEY (subasta)  REFERENCES subastas (identificador),
    CONSTRAINT fk_registroDeSubasta_duenios  FOREIGN KEY (duenio)   REFERENCES duenios (identificador),
    CONSTRAINT fk_registroDeSubasta_producto FOREIGN KEY (producto) REFERENCES productos (identificador),
    CONSTRAINT fk_registroDeSubasta_cliente  FOREIGN KEY (cliente)  REFERENCES clientes (identificador)
);
