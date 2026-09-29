# Roteiro de gravação — Projeto DimDim (2º Checkpoint)

**Grupo lupeol** · RM561940 Pedro França · RM563558 Olavo Neves · RM564495 Luiz Gonçalves
FIAP — DevOps Tools & Cloud Computing — Aplicativo e Banco em Nuvem

> Este arquivo cuida da **narrativa**: o que mostrar, em que ordem, o que
> dizer. As consultas SQL de evidência estão em
> [`EVIDENCIAS_VIDEO.sql`](EVIDENCIAS_VIDEO.sql). Deixe-o aberto no Query
> Editor durante a gravação.

O enunciado pede o vídeo **mostrando a execução completa do How To**:
criação dos recursos, deploy, testes com persistência e monitoramento do App
e do Banco. Por isso a gravação começa **com o ambiente vazio**.

---

## Na véspera: ensaio e ambiente limpo

- [ ] Rodar o How To inteiro uma vez (`01` a `08`) e conferir que tudo passa
- [ ] Apagar tudo com `./scripts/99_cleanup.sh` (digite `APAGAR`); o script
      também purga o Key Vault, liberando o nome para a gravação
- [ ] Confirmar que o grupo não existe mais: `az group show -n rg-dimdim-webapp-rm561940` deve dar erro

## Antes de apertar o REC

- [ ] Gravação em **1080p** (mínimo exigido: 720p), microfone testado, **explicação falada**
- [ ] Fonte do terminal **aumentada** e navegador com zoom de 125%
- [ ] `az login` feito e `az account show` conferido **fora da gravação**
- [ ] Terminal (Git Bash) aberto **na raiz do repositório clonado**
- [ ] Portal Azure aberto na lista de grupos de recursos
- [ ] Abas do navegador preparadas (vazias por enquanto): aplicação, Query
      Editor, Application Insights

---

## 1. Abertura (≈ 1 min)

- Apresentar o grupo e o desafio: Web App Java + Azure SQL + Application Insights.
- Mostrar o `README.md` no GitHub: descrição, **desenho da arquitetura**
  (explicar os fluxos 1–6) e a pasta `scripts/` com o DDL e os scripts do CLI.

## 2. Criação dos recursos em nuvem (≈ 8 min, com esperas)

Rodar e explicar cada script. Pode pausar a gravação durante as esperas
longas e retomar no resultado.

```bash
./scripts/01_resource-group.sh
./scripts/02_key-vault.sh        # "as senhas nascem aqui, ninguém as vê"
./scripts/03_sql-server.sh       # "banco PaaS, não container; firewall com meu IP"
./scripts/04_sql-schema.sh       # "DDL do scripts/DDL.sql + usuário de menor privilégio"
./scripts/05_app-insights.sh
./scripts/06_webapp.sh           # destacar a Key Vault reference e o ~3 do agente
```

Depois, no Portal: abrir `rg-dimdim-webapp-rm561940` e mostrar os recursos
criados (Web App, plano, SQL server, banco, Key Vault, App Insights, Log
Analytics). No Web App → *Environment variables*, mostrar a senha como Key
Vault reference com o **check verde**.

## 3. Deploy (≈ 3 min)

```bash
./scripts/07_deploy.sh
```

Explicar: o `mvnw` roda os testes e gera o JAR; o `az webapp deploy` publica;
o script espera a página responder 200. Abrir a URL no navegador e mostrar o
painel com os dados da carga inicial.

## 4. Testes com persistência (≈ 8 min) — o coração da nota

Antes: no Portal, `db_dimdim` → **Query editor**, login `user_dimdim`.

> Para não mostrar a senha: copie-a com
> `az keyvault secret show --vault-name kv-dimdim-web-rm561940 --name sql-app-password --query value -o tsv | clip`
> com a gravação **pausada**, cole no Query Editor e retome.

Colar o [`EVIDENCIAS_VIDEO.sql`](EVIDENCIAS_VIDEO.sql) e seguir bloco a bloco:
**ação na tela → SELECT**, para cada operação, em **cada tabela**:

| Tabela | Operação | Na tela | SELECT mostra |
|---|---|---|---|
| cliente | Create | Novo cliente Joana | a linha nova |
| cliente | Update | Editar nome e e-mail | a linha alterada |
| transacao | Create | Nova transação para Joana | a linha com o nome do cliente |
| transacao | Update | Editar descrição e valor | a linha alterada |
| cliente | Delete bloqueado | Excluir Joana → aviso | o cliente ainda existe |
| transacao | Delete | Excluir a transação | consulta vazia |
| cliente | Delete | Excluir Joana | consulta vazia |

Depois, **uma vez pela API** (curl no terminal), POST e DELETE de cliente,
com o SELECT correspondente (bloco "API REST" do `EVIDENCIAS_VIDEO.sql`).

**Obrigatório antes da seção 5:** gerar as falhas que o Application Insights
vai mostrar. A exclusão bloqueada pela tela devolve um redirect (302), não um
409; o 409 e o 404 só aparecem pela API:

```bash
URL=https://rm561940-webapp-dimdim.azurewebsites.net
curl -i -X DELETE $URL/api/clientes/1        # 409: Ana Souza (seed) tem transação
curl -i $URL/api/clientes/999999             # 404: cliente inexistente
```

Opcional: `./scripts/08_smoke-tests.sh` para mostrar as 17 verificações
contra a URL pública.

## 5. Monitoramento do App e do Banco (≈ 4 min)

Aguarde 2–3 minutos depois do CRUD (latência de ingestão). Em
`appi-dimdim-rm561940`:

1. **Live Metrics**: clicar na aplicação e ver as requisições subindo.
2. **Application Map**: a seta Web App → `sql-dimdim-rm561940` com contagem de chamadas.
3. **Transaction search**: abrir um `POST /transacoes` e mostrar a
   dependência SQL com o `INSERT` e a duração.
4. **Failures**: o 409 e o 404 gerados pelos dois `curl` obrigatórios do fim da seção 4.
5. **Performance**: tempo médio de resposta por operação.

Em `db_dimdim` → **Metrics**: DTU percentage e *Successful connections* no
período da gravação, que mostram o monitoramento do lado do banco.

## 6. Encerramento (≈ 30 s)

Recapitular: recursos por CLI, deploy automatizado com `az webapp deploy`,
persistência comprovada por SELECT em cada operação e monitoramento no
Application Insights. **Não** rodar o `99_cleanup.sh` até a correção.

---

## Problemas comuns durante a gravação

| Sintoma | Causa | Saída |
|---|---|---|
| Query Editor: "Client with IP address ... is not allowed" | IP mudou desde o `03` | rodar `./scripts/03_sql-server.sh` de novo |
| `02` falha: cofre excluído reversivelmente | cleanup antigo sem purge | `az keyvault purge --name kv-dimdim-web-rm561940` |
| `07` não chega ao 200 | app ainda iniciando ou erro de banco | `az webapp log tail -g rg-dimdim-webapp-rm561940 -n rm561940-webapp-dimdim` |
| Application Insights vazio | ingestão atrasada | esperar mais 2 minutos e atualizar |
| `argument --resource-group: expected one argument` | terminal fora da raiz do repo | `cd` para a raiz e repetir |
