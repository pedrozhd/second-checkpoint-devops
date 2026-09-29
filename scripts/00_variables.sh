#!/usr/bin/env bash
#
# Projeto DimDim - Grupo lupeol
# FIAP - DevOps Tools & Cloud Computing - 2o Checkpoint (Web App + Azure SQL)
#
#   RM561940 - Pedro Franca   (representante)
#   RM563558 - Olavo Neves
#   RM564495 - Luiz Goncalves
#
# Centraliza os nomes de todos os recursos. NENHUMA SENHA AQUI.
# As senhas sao geradas em 02_key-vault.sh e lidas do Key Vault em runtime.
#
# Este arquivo nao e executado diretamente: e carregado com `source` pelos
# demais scripts.
#
set -euo pipefail

# O Git Bash do Windows converte argumentos que parecem caminho absoluto
# ("/subscriptions/...") para a forma nativa ("C:/Program Files/Git/...")
# antes de repassa-los ao az.cmd. IDs de recurso da Azure comecam com "/",
# entao a traducao fica desligada em todos os scripts. Em Linux e macOS as
# variaveis sao inofensivas.
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"

# --- identificacao -----------------------------------------------------
export RM="rm561940"

# --- regiao ------------------------------------------------------------
# Permitida pela policy "Allowed resource deployment regions" da assinatura
# (canadacentral, northcentralus, brazilsouth, eastus, chilecentral) e
# confirmada na Fase 0 para Azure SQL Basic e App Service B1 Linux.
export LOCATION="brazilsouth"

# --- grupo de recursos -------------------------------------------------
# Diferente do grupo do CP1 (rg-dimdim-rm561940): o cleanup deste projeto
# nunca toca nos recursos do checkpoint anterior.
export RESOURCE_GROUP="rg-dimdim-webapp-${RM}"

# --- cofre -------------------------------------------------------------
# Nome diferente do cofre do CP1: aquele pode estar preso em soft delete.
export KEYVAULT_NAME="kv-dimdim-web-${RM}"
export SECRET_SQL_ADMIN_PASSWORD="sql-admin-password"
export SECRET_SQL_APP_PASSWORD="sql-app-password"

# --- banco de dados (Azure SQL, PaaS) -----------------------------------
export SQL_SERVER="sql-dimdim-${RM}"
export SQL_DATABASE="db_dimdim"
export SQL_ADMIN_USER="sqladmin_dimdim"   # so aplica o DDL (04)
export SQL_APP_USER="user_dimdim"         # usado pela aplicacao

# --- monitoramento -----------------------------------------------------
export LOG_WORKSPACE="log-dimdim-${RM}"
export APP_INSIGHTS="appi-dimdim-${RM}"

# --- aplicacao (App Service) -------------------------------------------
export APP_SERVICE_PLAN="asp-dimdim-${RM}"
export WEBAPP_NAME="${RM}-webapp-dimdim"
export WEBAPP_RUNTIME="JAVA:21-java21"

# --- tags --------------------------------------------------------------
export TAG_DISCIPLINA="devops-tools-cloud-computing"
export TAG_CHECKPOINT="cp2-webapp-banco"
export TAG_GRUPO="lupeol"
TAGS=("disciplina=${TAG_DISCIPLINA}" "checkpoint=${TAG_CHECKPOINT}" "grupo=${TAG_GRUPO}")

# --- derivados ---------------------------------------------------------
export SQL_FQDN="${SQL_SERVER}.database.windows.net"
export WEBAPP_URL="https://${WEBAPP_NAME}.azurewebsites.net"

# --- utilitarios usados pelos demais scripts ---------------------------

# Imprime um cabecalho de secao.
titulo() {
    echo ""
    echo "======================================================================"
    echo "  $*"
    echo "======================================================================"
}

# Aborta se um comando obrigatorio nao estiver disponivel.
exigir_comando() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERRO: comando '$1' nao encontrado no PATH." >&2
        exit 1
    fi
}

# Le um segredo do Key Vault. O valor vai para stdout e nunca para disco.
# O tr remove o \r que o az.cmd acrescenta no Git Bash do Windows.
ler_segredo() {
    az keyvault secret show \
        --vault-name "${KEYVAULT_NAME}" \
        --name "$1" \
        --query value \
        --output tsv | tr -d '\r'
}

# Gera uma senha forte sem caracteres que quebrem URL JDBC, T-SQL ou shell.
# O Azure SQL exige 3 de 4 categorias; o laco garante maiuscula, minuscula
# e digito.
gerar_senha() {
    local senha
    while :; do
        senha="$(openssl rand -base64 48 | tr -d '/+=\n' | head -c 28)"
        if [[ "${senha}" =~ [[:upper:]] && "${senha}" =~ [[:lower:]] && "${senha}" =~ [[:digit:]] ]]; then
            break
        fi
    done
    printf '%s' "${senha}"
}

# Imprime o caminho de um diretorio na forma que executaveis Windows
# (sqlcmd.exe, az.cmd) entendem. No Git Bash, `pwd -W` devolve C:/...;
# em Linux e macOS a opcao nao existe e o caminho original e mantido.
caminho_nativo() {
    local nativo
    if nativo="$(cd "$1" && pwd -W 2>/dev/null)"; then
        printf '%s' "${nativo}"
    else
        (cd "$1" && pwd)
    fi
}

# IP publico de quem executa o script (para a regra de firewall do SQL).
ip_publico() {
    curl -s --max-time 10 https://api.ipify.org | tr -d '\r\n'
}
