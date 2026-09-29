/* =====================================================================
   Projeto DimDim - usuario da aplicacao (menor privilegio)
   Grupo lupeol - RM561940 / RM563558 / RM564495

   Cria o contained database user que a aplicacao usa. Ele le e grava
   dados (db_datareader + db_datawriter), mas NAO altera estrutura:
   o DDL e aplicado apenas com o login administrador.

   Nenhuma senha neste arquivo. $(SQL_APP_USER) e $(APP_PASSWORD) sao
   variaveis do sqlcmd, lidas do ambiente que o 04_sql-schema.sh exporta
   com o valor vindo do Azure Key Vault.

   Idempotente: se o usuario ja existe, a senha e realinhada com o cofre.
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'$(SQL_APP_USER)')
    CREATE USER [$(SQL_APP_USER)] WITH PASSWORD = N'$(APP_PASSWORD)';
ELSE
    ALTER USER [$(SQL_APP_USER)] WITH PASSWORD = N'$(APP_PASSWORD)';
GO

IF IS_ROLEMEMBER(N'db_datareader', N'$(SQL_APP_USER)') = 0
    ALTER ROLE db_datareader ADD MEMBER [$(SQL_APP_USER)];
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'$(SQL_APP_USER)') = 0
    ALTER ROLE db_datawriter ADD MEMBER [$(SQL_APP_USER)];
GO
