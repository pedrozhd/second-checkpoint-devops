/* =====================================================================
   Projeto DimDim - Evidencias do CRUD para a gravacao do video
   Grupo lupeol - RM561940 / RM563558 / RM564495

   ONDE RODAR
     Portal Azure > SQL databases > db_dimdim > Query editor (preview)
     Autenticacao SQL: login user_dimdim (senha no Key Vault; faca o login
     ANTES de comecar a gravar, para a senha nao aparecer no video).

   COMO USAR
     Onde houver "NA TELA", faca a acao na aplicacao
       https://rm561940-webapp-dimdim.azurewebsites.net
     e SO DEPOIS rode o SELECT que vem logo abaixo (selecione a consulta e
     clique em Run). A ordem e sempre a mesma: a aplicacao altera, o
     SELECT prova.

   As consultas filtram pelo CPF de teste (32132132100), entao funcionam
   sem anotar ids.
   ===================================================================== */


/* =====================================================================
   ESTADO INICIAL
   ===================================================================== */

-- Prova que a conexao e com o Azure SQL, no banco e usuario certos.
SELECT @@SERVERNAME AS servidor, DB_NAME() AS banco, SUSER_SNAME() AS usuario_conectado;

SELECT * FROM dbo.cliente   ORDER BY id_cliente;
SELECT * FROM dbo.transacao ORDER BY id_transacao;


/* =====================================================================
   CLIENTE
   ===================================================================== */

-- --- CREATE ----------------------------------------------------------
-- NA TELA: Clientes > + Novo cliente
--   Nome: Joana Prado | CPF: 32132132100 | E-mail: joana.prado@dimdim.com > Salvar

SELECT * FROM dbo.cliente WHERE cpf = '32132132100';


-- --- UPDATE ----------------------------------------------------------
-- NA TELA: Clientes > Editar (Joana Prado)
--   Nome: Joana Prado Martins | E-mail: joana.martins@dimdim.com > Salvar

SELECT * FROM dbo.cliente WHERE cpf = '32132132100';

-- O DELETE do cliente vem depois, por causa da chave estrangeira.


/* =====================================================================
   TRANSACAO
   ===================================================================== */

-- --- CREATE ----------------------------------------------------------
-- NA TELA: Transacoes > + Nova transacao
--   Cliente: Joana Prado Martins | Descricao: Transferência recebida
--   Valor: 890.25 | Tipo: Crédito > Salvar

SELECT t.*, c.nome AS nome_cliente
  FROM dbo.transacao t
  JOIN dbo.cliente   c ON c.id_cliente = t.id_cliente
 WHERE c.cpf = '32132132100';


-- --- UPDATE ----------------------------------------------------------
-- NA TELA: Transacoes > Editar (Transferência recebida)
--   Descricao: Transferência recebida - CORRIGIDA | Valor: 1120.75 > Salvar

SELECT t.*, c.nome AS nome_cliente
  FROM dbo.transacao t
  JOIN dbo.cliente   c ON c.id_cliente = t.id_cliente
 WHERE c.cpf = '32132132100';


/* =====================================================================
   INTEGRIDADE REFERENCIAL - cliente com transacao nao pode ser apagado
   ===================================================================== */

-- NA TELA: Clientes > Excluir (Joana Prado Martins) > OK
--   A tela mostra: "Não é possível excluir o cliente: existem transações
--   vinculadas a ele..."

-- O cliente continua no banco - a FK e ON DELETE NO ACTION:
SELECT * FROM dbo.cliente WHERE cpf = '32132132100';


/* =====================================================================
   DELETE, NA ORDEM CORRETA
   ===================================================================== */

-- --- DELETE da transacao ---------------------------------------------
-- NA TELA: Transacoes > Excluir (Transferência recebida - CORRIGIDA) > OK

-- A consulta volta vazia: e essa a evidencia da exclusao.
SELECT t.*
  FROM dbo.transacao t
  JOIN dbo.cliente   c ON c.id_cliente = t.id_cliente
 WHERE c.cpf = '32132132100';


-- --- DELETE do cliente -----------------------------------------------
-- NA TELA: Clientes > Excluir (Joana Prado Martins) > OK

-- Vazia: o cliente foi removido.
SELECT * FROM dbo.cliente WHERE cpf = '32132132100';


/* =====================================================================
   API REST (uma vez, para mostrar que a API grava no mesmo banco)
   ===================================================================== */

-- NO TERMINAL:
--   curl -i -X POST https://rm561940-webapp-dimdim.azurewebsites.net/api/clientes \
--     -H "Content-Type: application/json" \
--     -d '{"nome":"Carlos Api","cpf":"45645645600","email":"carlos.api@dimdim.com"}'

SELECT * FROM dbo.cliente WHERE cpf = '45645645600';

-- NO TERMINAL (use o idCliente devolvido pelo POST):
--   curl -i -X DELETE https://rm561940-webapp-dimdim.azurewebsites.net/api/clientes/<id>

SELECT * FROM dbo.cliente WHERE cpf = '45645645600';


/* =====================================================================
   FECHAMENTO - o saldo do painel bate com o banco
   ===================================================================== */

SELECT SUM(CASE WHEN tipo = 'CREDITO' THEN valor ELSE -valor END) AS saldo_geral
  FROM dbo.transacao;
