#!/usr/bin/env bash
#
# 07 - Compila a aplicacao e publica o JAR no Web App (az webapp deploy).
#
#   1. ./mvnw -B clean package   -> roda os testes e gera app/target/dimdim.jar
#   2. az webapp deploy --type jar
#      O App Service (OneDeploy) renomeia o arquivo para app.jar em
#      /home/site/wwwroot e reinicia a aplicacao.
#   3. Aguarda a pagina inicial responder 200. A pagina consulta o banco,
#      entao o 200 prova a cadeia inteira: Web App -> Key Vault -> Azure SQL.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando java
exigir_comando curl

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"

titulo "07 - DEPLOY"
echo "Web App : ${WEBAPP_NAME}"
echo "URL     : ${WEBAPP_URL}"

titulo "BUILD (com testes)"
(cd "${RAIZ}/app" && ./mvnw -B clean package)

JAR="$(caminho_nativo "${RAIZ}/app/target")/dimdim.jar"
echo ""
echo "Artefato: ${JAR}"

titulo "PUBLICACAO NO APP SERVICE"
az webapp deploy \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_NAME}" \
    --src-path "${JAR}" \
    --type jar \
    --timeout 600000 \
    --output none
echo "  JAR publicado."

titulo "AGUARDANDO A APLICACAO RESPONDER"
echo "Timeout: 5 minutos. O primeiro start da JVM leva de 1 a 2 minutos."

PRONTO=0
for tentativa in $(seq 1 30); do
    CODIGO="$(curl -s -o /dev/null -w "%{http_code}" --max-time 20 "${WEBAPP_URL}/" || true)"
    if [[ "${CODIGO}" == "200" ]]; then
        echo ""
        echo "Aplicacao respondeu 200 apos aproximadamente $((tentativa * 10)) segundos."
        PRONTO=1
        break
    fi
    printf "."
    sleep 10
done

if [[ "${PRONTO}" -eq 0 ]]; then
    echo ""
    echo "ERRO: a aplicacao nao respondeu 200 em 5 minutos (ultimo codigo: ${CODIGO})." >&2
    echo "Veja o log com:" >&2
    echo "    az webapp log tail -g ${RESOURCE_GROUP} -n ${WEBAPP_NAME}" >&2
    exit 1
fi

titulo "RESULTADO"
echo "Telas : ${WEBAPP_URL}/"
echo "        ${WEBAPP_URL}/clientes"
echo "        ${WEBAPP_URL}/transacoes"
echo "API   : ${WEBAPP_URL}/api/clientes"
echo "        ${WEBAPP_URL}/api/transacoes"
