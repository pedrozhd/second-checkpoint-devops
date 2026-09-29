#!/usr/bin/env bash
#
# 05 - Cria o Log Analytics workspace e o Application Insights.
#
# O Application Insights "workspace-based" guarda a telemetria no Log
# Analytics. A ligacao com o Web App acontece no 06, por app settings:
# o proprio App Service injeta o agente Java 3.x, sem mudar o codigo.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "05 - APPLICATION INSIGHTS"
echo "Workspace : ${LOG_WORKSPACE}"
echo "Insights  : ${APP_INSIGHTS}"

if ! az extension show --name application-insights >/dev/null 2>&1; then
    echo "Instalando a extensao application-insights do az..."
    az extension add --name application-insights --only-show-errors
fi

titulo "LOG ANALYTICS WORKSPACE"
if az monitor log-analytics workspace show --resource-group "${RESOURCE_GROUP}" \
                                           --workspace-name "${LOG_WORKSPACE}" >/dev/null 2>&1; then
    echo "AVISO: o workspace '${LOG_WORKSPACE}' ja existe. Nada a fazer."
else
    az monitor log-analytics workspace create \
        --resource-group "${RESOURCE_GROUP}" \
        --workspace-name "${LOG_WORKSPACE}" \
        --location "${LOCATION}" \
        --retention-time 30 \
        --tags "${TAGS[@]}" \
        --output none
    echo "  workspace .................... criado"
fi

WORKSPACE_ID="$(az monitor log-analytics workspace show \
                    --resource-group "${RESOURCE_GROUP}" \
                    --workspace-name "${LOG_WORKSPACE}" \
                    --query id --output tsv | tr -d '\r')"

titulo "APPLICATION INSIGHTS"
if az monitor app-insights component show --resource-group "${RESOURCE_GROUP}" \
                                          --app "${APP_INSIGHTS}" >/dev/null 2>&1; then
    echo "AVISO: o componente '${APP_INSIGHTS}' ja existe. Nada a fazer."
else
    az monitor app-insights component create \
        --resource-group "${RESOURCE_GROUP}" \
        --app "${APP_INSIGHTS}" \
        --location "${LOCATION}" \
        --kind web \
        --application-type web \
        --workspace "${WORKSPACE_ID}" \
        --tags "${TAGS[@]}" \
        --output none
    echo "  application insights ......... criado"
fi

titulo "RESULTADO"
az monitor app-insights component show \
    --resource-group "${RESOURCE_GROUP}" \
    --app "${APP_INSIGHTS}" \
    --query "{nome:name, regiao:location, tipo:applicationType, estado:provisioningState}" \
    --output table

echo ""
echo "A connection string NAO e exibida aqui. O 06 a le em runtime e a"
echo "grava como app setting do Web App."
