#!/usr/bin/env bash
#
# 99 - Remove TODOS os recursos do projeto na Azure.
#
# ############################################################
# #  ATENCAO: ESTE SCRIPT E DESTRUTIVO E IRREVERSIVEL.       #
# #                                                          #
# #  Apaga o grupo de recursos inteiro: o Web App, o plano,  #
# #  o Azure SQL com TODOS os dados, o Application Insights, #
# #  o Log Analytics e o Key Vault (que ainda e purgado).    #
# #                                                          #
# #  NAO EXECUTE depois de gravar o video e antes da         #
# #  correcao: as evidencias da entrega desaparecem junto.   #
# ############################################################
#
# Exige confirmacao digitada. Nao ha modo silencioso.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "99 - REMOCAO DO AMBIENTE"

if ! az group show --name "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo "O grupo '${RESOURCE_GROUP}' nao existe. Nada a remover."
    exit 0
fi

echo "Os recursos abaixo serao APAGADOS:"
echo ""
az resource list --resource-group "${RESOURCE_GROUP}" \
    --query "[].{nome:name, tipo:type}" --output table

echo ""
echo "Isso inclui o banco ${SQL_DATABASE} com todos os dados."
echo "Depois de apagar o grupo, o cofre '${KEYVAULT_NAME}' sera PURGADO"
echo "(sem purge, o soft delete prende o nome por 90 dias e o 02 falha)."
echo ""

read -r -p "Digite exatamente APAGAR para confirmar: " confirmacao
if [[ "${confirmacao}" != "APAGAR" ]]; then
    echo "Cancelado. Nenhum recurso foi removido."
    exit 0
fi

titulo "REMOVENDO O GRUPO (aguarde alguns minutos)"
az group delete --name "${RESOURCE_GROUP}" --yes
echo "Grupo removido."

titulo "PURGANDO O KEY VAULT"
if az keyvault list-deleted --query "[?name=='${KEYVAULT_NAME}'].name" \
                            --output tsv 2>/dev/null | grep -q .; then
    az keyvault purge --name "${KEYVAULT_NAME}"
    echo "Cofre purgado. O nome '${KEYVAULT_NAME}' esta livre de novo."
else
    echo "Nenhum cofre '${KEYVAULT_NAME}' em soft delete. Nada a purgar."
fi
