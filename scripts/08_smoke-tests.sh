#!/usr/bin/env bash
#
# 08 - Exercita as telas e os 10 endpoints da API contra a URL PUBLICA.
#
# Todas as chamadas partem da maquina local e usam o endereco
# *.azurewebsites.net - nada de localhost. Cada operacao imprime o status
# HTTP esperado ao lado do obtido. Os registros criados sao apagados no fim.
#
# Projeto DimDim - Grupo lupeol - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando curl

titulo "08 - SMOKE TESTS EM NUVEM"

API="${WEBAPP_URL}/api"
echo "Endpoint base: ${WEBAPP_URL}"

# O arquivo temporario fica ao lado do script, e nao em /tmp: no Git Bash do
# Windows o curl.exe nao enxerga o /tmp emulado.
CORPO="$(dirname "$0")/.smoke-resposta.tmp"
trap 'rm -f "${CORPO}"' EXIT

FALHAS=0

# chamar <esperado> <rotulo> <args do curl...>
chamar() {
    local esperado="$1"; shift
    local rotulo="$1"; shift
    local obtido
    obtido=$(curl -s -o "${CORPO}" -w "%{http_code}" --max-time 30 "$@")

    if [[ "${obtido}" == "${esperado}" ]]; then
        printf "  [ OK ] %-50s %s\n" "${rotulo}" "${obtido}"
    else
        printf "  [FALHA] %-49s esperado %s, obtido %s\n" "${rotulo}" "${esperado}" "${obtido}"
        FALHAS=$((FALHAS + 1))
    fi
}

titulo "TELAS"
chamar 200 "GET  / (painel)"        "${WEBAPP_URL}/"
chamar 200 "GET  /clientes"         "${WEBAPP_URL}/clientes"
chamar 200 "GET  /transacoes"       "${WEBAPP_URL}/transacoes"
chamar 404 "GET  /clientes/0/editar (inexistente)" "${WEBAPP_URL}/clientes/0/editar"

titulo "TABELA CLIENTE"

# O CPF e UNIQUE. Derivado do timestamp para o teste ser reexecutavel.
CPF_TESTE="$(date +%s | tail -c 10)00"
CPF_TESTE="${CPF_TESTE:0:11}"

chamar 201 "POST   /api/clientes" \
    -X POST "${API}/clientes" \
    -H "Content-Type: application/json" \
    -d "{\"nome\":\"Cliente Smoke Test\",\"cpf\":\"${CPF_TESTE}\",\"email\":\"smoke@dimdim.com\"}"

ID_CLIENTE=$(grep -o '"idCliente":[0-9]*' "${CORPO}" | head -1 | cut -d: -f2)
echo "         id_cliente criado: ${ID_CLIENTE}"

chamar 200 "GET    /api/clientes (lista)"          "${API}/clientes"
chamar 200 "GET    /api/clientes/${ID_CLIENTE}"    "${API}/clientes/${ID_CLIENTE}"

chamar 200 "PUT    /api/clientes/${ID_CLIENTE}" \
    -X PUT "${API}/clientes/${ID_CLIENTE}" \
    -H "Content-Type: application/json" \
    -d "{\"nome\":\"Cliente Smoke Test ALTERADO\",\"cpf\":\"${CPF_TESTE}\",\"email\":\"smoke.alterado@dimdim.com\"}"

titulo "TABELA TRANSACAO"

chamar 201 "POST   /api/transacoes" \
    -X POST "${API}/transacoes" \
    -H "Content-Type: application/json" \
    -d "{\"idCliente\":${ID_CLIENTE},\"descricao\":\"Transacao Smoke Test\",\"valor\":199.90,\"tipo\":\"CREDITO\"}"

ID_TRANSACAO=$(grep -o '"idTransacao":[0-9]*' "${CORPO}" | head -1 | cut -d: -f2)
echo "         id_transacao criada: ${ID_TRANSACAO}"

chamar 200 "GET    /api/transacoes (lista)"        "${API}/transacoes"
chamar 200 "GET    /api/transacoes/${ID_TRANSACAO}" "${API}/transacoes/${ID_TRANSACAO}"

chamar 200 "PUT    /api/transacoes/${ID_TRANSACAO}" \
    -X PUT "${API}/transacoes/${ID_TRANSACAO}" \
    -H "Content-Type: application/json" \
    -d "{\"idCliente\":${ID_CLIENTE},\"descricao\":\"Transacao Smoke Test ALTERADA\",\"valor\":250.00,\"tipo\":\"DEBITO\"}"

titulo "INTEGRIDADE REFERENCIAL"

# A FK e ON DELETE NO ACTION: apagar o cliente antes da transacao precisa
# devolver 409, e nao 500.
chamar 409 "DELETE /api/clientes/${ID_CLIENTE} (com transacao)" \
    -X DELETE "${API}/clientes/${ID_CLIENTE}"

titulo "EXCLUSAO NA ORDEM CORRETA"

chamar 204 "DELETE /api/transacoes/${ID_TRANSACAO}" -X DELETE "${API}/transacoes/${ID_TRANSACAO}"
chamar 404 "GET    /api/transacoes/${ID_TRANSACAO} (apos delete)" "${API}/transacoes/${ID_TRANSACAO}"
chamar 204 "DELETE /api/clientes/${ID_CLIENTE}"     -X DELETE "${API}/clientes/${ID_CLIENTE}"
chamar 404 "GET    /api/clientes/${ID_CLIENTE} (apos delete)"     "${API}/clientes/${ID_CLIENTE}"

titulo "RESUMO"
if [[ "${FALHAS}" -eq 0 ]]; then
    echo "Todos os testes passaram."
else
    echo "${FALHAS} teste(s) falharam." >&2
    exit 1
fi
