#!/usr/bin/env bash
#
# 02 - Cria o Azure Key Vault e gera as senhas do Azure SQL.
#
# As senhas sao geradas AQUI, em tempo de execucao, com `openssl rand`.
# Nenhum valor literal existe em arquivo:
#   - sql-admin-password : login administrador do servidor (03 e 04)
#   - sql-app-password   : usuario da aplicacao user_dimdim (04 e 06)
# O Web App nunca recebe a senha em texto: recebe uma Key Vault reference,
# resolvida pela Managed Identity dele (06).
#
# ATENCAO - soft delete: um cofre apagado fica "excluido reversivelmente"
# por 90 dias e prende o nome. O 99_cleanup.sh faz o purge logo apos
# apagar o grupo, para que a gravacao do video possa recriar tudo do zero.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando openssl

titulo "02 - AZURE KEY VAULT"
echo "Cofre : ${KEYVAULT_NAME}"

if az keyvault show --name "${KEYVAULT_NAME}" \
                    --resource-group "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo ""
    echo "AVISO: o cofre '${KEYVAULT_NAME}' ja existe. Nada a fazer."
else
    if az keyvault list-deleted --query "[?name=='${KEYVAULT_NAME}'].name" \
                                --output tsv 2>/dev/null | grep -q .; then
        echo ""
        echo "ERRO: existe um cofre '${KEYVAULT_NAME}' excluido reversivelmente." >&2
        echo "Para reutilizar o nome e preciso purga-lo:" >&2
        echo "    az keyvault purge --name ${KEYVAULT_NAME}" >&2
        echo "Isso e IRREVERSIVEL. Confirme com o responsavel antes." >&2
        exit 1
    fi

    # Access policies (e nao RBAC): o 06 concede `get` a Managed Identity do
    # Web App com um unico `az keyvault set-policy`.
    az keyvault create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${KEYVAULT_NAME}" \
        --location "${LOCATION}" \
        --enable-rbac-authorization false \
        --tags "${TAGS[@]}" \
        --output table
fi

titulo "SEGREDOS"

# Cada segredo so e criado se ainda nao existir: reexecutar o script nao
# troca a senha de um banco que ja esta no ar.
criar_segredo_se_ausente() {
    local nome="$1"
    if az keyvault secret show --vault-name "${KEYVAULT_NAME}" \
                               --name "${nome}" >/dev/null 2>&1; then
        echo "  ${nome} .......... ja existe (mantido)"
    else
        az keyvault secret set \
            --vault-name "${KEYVAULT_NAME}" \
            --name "${nome}" \
            --value "$(gerar_senha)" \
            --output none
        echo "  ${nome} .......... criado"
    fi
}

criar_segredo_se_ausente "${SECRET_SQL_ADMIN_PASSWORD}"
criar_segredo_se_ausente "${SECRET_SQL_APP_PASSWORD}"

titulo "RESULTADO"
az keyvault secret list \
    --vault-name "${KEYVAULT_NAME}" \
    --query "[].{segredo:name, habilitado:attributes.enabled}" \
    --output table

echo ""
echo "Os VALORES dos segredos nunca sao impressos nem gravados em disco."
