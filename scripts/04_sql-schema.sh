#!/usr/bin/env bash
#
# 04 - Aplica o schema no Azure SQL com o sqlcmd.
#
# Ordem: DDL.sql -> seed.sql -> app-user.sql
#
# A senha do administrador vai para o sqlcmd pela variavel de ambiente
# SQLCMDPASSWORD (e nao por -P): assim ela nao aparece na lista de
# processos nem no historico do shell. A senha do usuario da aplicacao e
# lida pelo app-user.sql como variavel de script $(APP_PASSWORD), tambem
# vinda do ambiente. As duas saem do Key Vault e sao apagadas ao final.
#
# -N          conexao criptografada (TLS)
# -b          aborta no primeiro erro
# -I          QUOTED_IDENTIFIER ON
# -f 65001    le os .sql como UTF-8 (acentos do seed)
#
# Pre-requisito: IP de quem executa liberado no firewall (03).
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando sqlcmd

DIR_SQL="$(caminho_nativo "$(dirname "$0")")"

titulo "04 - SCHEMA DO BANCO"
echo "Servidor : ${SQL_FQDN}"
echo "Banco    : ${SQL_DATABASE}"
echo "Arquivos : ${DIR_SQL}/{DDL,seed,app-user}.sql"

trap 'unset SQLCMDPASSWORD APP_PASSWORD' EXIT

titulo "LEITURA DE CREDENCIAIS (runtime, nada em disco)"
SQLCMDPASSWORD="$(ler_segredo "${SECRET_SQL_ADMIN_PASSWORD}")"
export SQLCMDPASSWORD
APP_PASSWORD="$(ler_segredo "${SECRET_SQL_APP_PASSWORD}")"
export APP_PASSWORD
echo "  senhas do Key Vault .......... lidas"

sql_admin() {
    sqlcmd -S "tcp:${SQL_FQDN},1433" -d "${SQL_DATABASE}" -U "${SQL_ADMIN_USER}" \
           -N -b -I -f 65001 "$@"
}

titulo "DDL (tabelas, constraints, indice)"
sql_admin -i "${DIR_SQL}/DDL.sql"
echo "  DDL.sql ...................... aplicado"

titulo "CARGA INICIAL"
sql_admin -i "${DIR_SQL}/seed.sql"
echo "  seed.sql ..................... aplicado"

titulo "USUARIO DA APLICACAO (${SQL_APP_USER})"
sql_admin -i "${DIR_SQL}/app-user.sql"
echo "  app-user.sql ................. aplicado"

titulo "RESULTADO"
sql_admin -W -s " | " -Q "SET NOCOUNT ON;
SELECT 'cliente' AS tabela, COUNT(*) AS linhas FROM dbo.cliente
UNION ALL
SELECT 'transacao', COUNT(*) FROM dbo.transacao;
SELECT dp.name AS usuario, r.name AS papel
  FROM sys.database_role_members m
  JOIN sys.database_principals r  ON r.principal_id  = m.role_principal_id
  JOIN sys.database_principals dp ON dp.principal_id = m.member_principal_id
 WHERE dp.name = '${SQL_APP_USER}';"
