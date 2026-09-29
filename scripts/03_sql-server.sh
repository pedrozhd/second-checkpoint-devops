#!/usr/bin/env bash
#
# 03 - Cria o Azure SQL (servidor logico + banco) e o firewall.
#
# Banco PaaS, nao containerizado (exigencia do enunciado).
#   - Edicao Basic (5 DTU, 2 GB): sempre ligada. O serverless gratuito
#     pausa sozinho, e o primeiro acesso depois da pausa demora ~1 minuto.
#   - Backup com redundancia local: suficiente para ambiente academico.
#   - Firewall:
#       AllowAzureServices (0.0.0.0) -> o Web App alcanca o banco
#       operador-AAAAMMDD            -> IP de quem roda o script, para o
#                                       sqlcmd (04) e o Query Editor do
#                                       Portal. Descoberto em runtime.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando curl

titulo "03 - AZURE SQL"
echo "Servidor : ${SQL_FQDN}"
echo "Banco    : ${SQL_DATABASE} (Basic)"
echo "Admin    : ${SQL_ADMIN_USER}"

titulo "SERVIDOR LOGICO"
if az sql server show --resource-group "${RESOURCE_GROUP}" \
                      --name "${SQL_SERVER}" >/dev/null 2>&1; then
    echo "AVISO: o servidor '${SQL_SERVER}' ja existe. Nada a fazer."
else
    SQL_ADMIN_PASSWORD="$(ler_segredo "${SECRET_SQL_ADMIN_PASSWORD}")"
    echo "  senha do administrador ...... lida do Key Vault"

    az sql server create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${SQL_SERVER}" \
        --location "${LOCATION}" \
        --admin-user "${SQL_ADMIN_USER}" \
        --admin-password "${SQL_ADMIN_PASSWORD}" \
        --minimal-tls-version 1.2 \
        --output none

    unset SQL_ADMIN_PASSWORD
    echo "  servidor .................... criado"
fi

titulo "BANCO DE DADOS"
if az sql db show --resource-group "${RESOURCE_GROUP}" \
                  --server "${SQL_SERVER}" \
                  --name "${SQL_DATABASE}" >/dev/null 2>&1; then
    echo "AVISO: o banco '${SQL_DATABASE}' ja existe. Nada a fazer."
else
    az sql db create \
        --resource-group "${RESOURCE_GROUP}" \
        --server "${SQL_SERVER}" \
        --name "${SQL_DATABASE}" \
        --edition Basic \
        --capacity 5 \
        --max-size 2GB \
        --backup-storage-redundancy Local \
        --tags "${TAGS[@]}" \
        --output none
    echo "  banco ....................... criado"
fi

titulo "FIREWALL"
# firewall-rule create e um upsert: reexecutar apenas atualiza a regra.
az sql server firewall-rule create \
    --resource-group "${RESOURCE_GROUP}" \
    --server "${SQL_SERVER}" \
    --name AllowAzureServices \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0 \
    --output none
echo "  AllowAzureServices .......... ok"

MEU_IP="$(ip_publico)"
if [[ -z "${MEU_IP}" ]]; then
    echo "ERRO: nao foi possivel descobrir o IP publico (api.ipify.org)." >&2
    exit 1
fi
REGRA_OPERADOR="operador-$(date +%Y%m%d)"
az sql server firewall-rule create \
    --resource-group "${RESOURCE_GROUP}" \
    --server "${SQL_SERVER}" \
    --name "${REGRA_OPERADOR}" \
    --start-ip-address "${MEU_IP}" \
    --end-ip-address "${MEU_IP}" \
    --output none
echo "  ${REGRA_OPERADOR} ........... ${MEU_IP}"

titulo "RESULTADO"
az sql db show \
    --resource-group "${RESOURCE_GROUP}" \
    --server "${SQL_SERVER}" \
    --name "${SQL_DATABASE}" \
    --query "{banco:name, edicao:edition, objetivo:currentServiceObjectiveName, estado:status}" \
    --output table
az sql server firewall-rule list \
    --resource-group "${RESOURCE_GROUP}" \
    --server "${SQL_SERVER}" \
    --query "[].{regra:name, inicio:startIpAddress, fim:endIpAddress}" \
    --output table
