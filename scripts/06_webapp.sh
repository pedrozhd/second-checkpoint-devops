#!/usr/bin/env bash
#
# 06 - Cria o App Service Plan e o Web App (Java 21 SE), liga a Managed
#      Identity ao Key Vault e configura as app settings.
#
# App Service, sem container (exigencia do enunciado): o 07 publica o JAR.
#
# Segredos:
#   SPRING_DATASOURCE_PASSWORD recebe uma Key Vault reference
#   "@Microsoft.KeyVault(SecretUri=...)". O App Service resolve a referencia
#   com a Managed Identity do Web App; a senha nunca aparece no Portal nem no
#   `az webapp config appsettings list`.
#
# Monitoramento:
#   APPLICATIONINSIGHTS_CONNECTION_STRING + ApplicationInsightsAgent_EXTENSION_VERSION=~3
#   fazem o App Service injetar o agente Java 3.x do Application Insights.
#
# Fuso horario:
#   JAVA_OPTS=-Duser.timezone=America/Sao_Paulo. O App Service roda em UTC e a
#   imagem Java nao traz tzdata (a variavel TZ e ignorada); a JVM usa a base de
#   fusos propria. Sem isto, o timestamp dos
#   erros da API e os logs sairiam 3 horas a frente do horario de Brasilia
#   (as datas gravadas ja vem do DEFAULT do banco, ver scripts/DDL.sql).
#
# As app settings vao por arquivo JSON (--settings @arquivo): os valores tem
# ';', '~' e parenteses, que se perdem na passagem Git Bash -> az.cmd.
# O arquivo nao contem senha e e apagado ao final.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "06 - WEB APP"
echo "Plano    : ${APP_SERVICE_PLAN} (Linux B1)"
echo "Web App  : ${WEBAPP_NAME}"
echo "Runtime  : ${WEBAPP_RUNTIME}"
echo "URL      : ${WEBAPP_URL}"

titulo "APP SERVICE PLAN"
if az appservice plan show --resource-group "${RESOURCE_GROUP}" \
                           --name "${APP_SERVICE_PLAN}" >/dev/null 2>&1; then
    echo "AVISO: o plano '${APP_SERVICE_PLAN}' ja existe. Nada a fazer."
else
    # B1: tem Always On. O F1 gratuito dorme e tem cota diaria de CPU.
    az appservice plan create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${APP_SERVICE_PLAN}" \
        --location "${LOCATION}" \
        --is-linux \
        --sku B1 \
        --tags "${TAGS[@]}" \
        --output none
    echo "  plano ........................ criado"
fi

titulo "WEB APP"
if az webapp show --resource-group "${RESOURCE_GROUP}" \
                  --name "${WEBAPP_NAME}" >/dev/null 2>&1; then
    echo "AVISO: o Web App '${WEBAPP_NAME}' ja existe. Nada a fazer."
else
    az webapp create \
        --resource-group "${RESOURCE_GROUP}" \
        --plan "${APP_SERVICE_PLAN}" \
        --name "${WEBAPP_NAME}" \
        --runtime "${WEBAPP_RUNTIME}" \
        --tags "${TAGS[@]}" \
        --output none
    echo "  web app ...................... criado"
fi

titulo "MANAGED IDENTITY -> KEY VAULT"
# identity assign e idempotente: devolve sempre o mesmo principalId.
PRINCIPAL_ID="$(az webapp identity assign \
                    --resource-group "${RESOURCE_GROUP}" \
                    --name "${WEBAPP_NAME}" \
                    --query principalId --output tsv | tr -d '\r')"
echo "  identidade ................... ${PRINCIPAL_ID}"

# Somente leitura de segredos: a identidade nao lista, nao cria, nao apaga.
az keyvault set-policy \
    --name "${KEYVAULT_NAME}" \
    --object-id "${PRINCIPAL_ID}" \
    --secret-permissions get \
    --output none
echo "  permissao 'get' em segredos .. concedida"

titulo "APP SETTINGS"
AI_CONNECTION_STRING="$(az monitor app-insights component show \
                            --resource-group "${RESOURCE_GROUP}" \
                            --app "${APP_INSIGHTS}" \
                            --query connectionString --output tsv | tr -d '\r')"

JDBC_URL="jdbc:sqlserver://${SQL_FQDN}:1433;database=${SQL_DATABASE};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
KV_REFERENCE="@Microsoft.KeyVault(SecretUri=https://${KEYVAULT_NAME}.vault.azure.net/secrets/${SECRET_SQL_APP_PASSWORD}/)"

DIR_SCRIPTS="$(caminho_nativo "$(dirname "$0")")"
ARQUIVO_SETTINGS="$(dirname "$0")/.appsettings.tmp.json"
trap 'rm -f "${ARQUIVO_SETTINGS}"' EXIT

cat > "${ARQUIVO_SETTINGS}" <<EOF
[
  { "name": "SPRING_DATASOURCE_URL",                      "value": "${JDBC_URL}",             "slotSetting": false },
  { "name": "SPRING_DATASOURCE_USERNAME",                 "value": "${SQL_APP_USER}",         "slotSetting": false },
  { "name": "SPRING_DATASOURCE_PASSWORD",                 "value": "${KV_REFERENCE}",         "slotSetting": false },
  { "name": "APPLICATIONINSIGHTS_CONNECTION_STRING",      "value": "${AI_CONNECTION_STRING}", "slotSetting": false },
  { "name": "ApplicationInsightsAgent_EXTENSION_VERSION", "value": "~3",                      "slotSetting": false },
  { "name": "JAVA_OPTS",                                  "value": "-Duser.timezone=America/Sao_Paulo", "slotSetting": false }
]
EOF

az webapp config appsettings set \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --settings "@${DIR_SCRIPTS}/.appsettings.tmp.json" \
    --output none
unset AI_CONNECTION_STRING
echo "  6 app settings ............... gravadas"

titulo "CONFIGURACAO DO SITE"
az webapp config set \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --always-on true \
    --min-tls-version 1.2 \
    --ftps-state Disabled \
    --output none
az webapp update \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --https-only true \
    --output none
az webapp log config \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --docker-container-logging filesystem \
    --output none
echo "  Always On, HTTPS only, TLS 1.2, FTPS desligado, log de container"

titulo "RESULTADO"
az webapp show \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --query "{webapp:name, estado:state, runtime:siteConfig.linuxFxVersion, url:defaultHostName, httpsOnly:httpsOnly}" \
    --output table

echo ""
echo "App settings (apenas nomes):"
az webapp config appsettings list \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --query "[].name" --output tsv | tr -d '\r' | sed 's/^/  /'

echo ""
echo "Valor de SPRING_DATASOURCE_PASSWORD (uma referencia, nao a senha):"
az webapp config appsettings list \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --query "[?name=='SPRING_DATASOURCE_PASSWORD'].value" --output tsv | tr -d '\r' | sed 's/^/  /'
