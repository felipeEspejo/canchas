/*
  Canchas (reservas por franja) - preparacion de Microsoft SQL Server (se ejecuta UNA sola vez).

  Ejecutar como administrador del servidor (por ejemplo con autenticacion de Windows):
    sqlcmd -S localhost\MSSQLSERVER01 -E -C -i database\crear-base.sql
  o abrirlo en SQL Server Management Studio y ejecutarlo.

  Antes, el servidor debe aceptar usuario y contrasena (modo mixto):
    SSMS > clic derecho en el servidor > Propiedades > Seguridad >
    "Modo de autenticacion de SQL Server y de Windows" > Aceptar, y reiniciar el servicio de SQL Server.

  Las tablas NO se crean aqui: las crea la aplicacion al arrancar (SqlServerSchema.createMissingTables)
  y DemoData carga una semana de ejemplo si la base esta vacia.
  El usuario y la contrasena deben coincidir con app.db.username y app.db.password de application.properties.
*/

-- 1. Base de datos
IF DB_ID(N'canchas') IS NULL
    CREATE DATABASE canchas;
GO

-- 2. Login del servidor (usuario y contrasena de la aplicacion)
IF SUSER_ID(N'canchas') IS NULL
    CREATE LOGIN canchas WITH PASSWORD = N'Canchas#2026dev', DEFAULT_DATABASE = canchas, CHECK_POLICY = ON;
GO

-- 3. Usuario dentro de la base, con permiso para crear tablas y leer/escribir datos
USE canchas;
GO
IF USER_ID(N'canchas') IS NULL
    CREATE USER canchas FOR LOGIN canchas;
GO
ALTER ROLE db_owner ADD MEMBER canchas;
GO
