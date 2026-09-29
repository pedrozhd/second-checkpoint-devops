/* =====================================================================
   Projeto DimDim - carga inicial (seed)
   Grupo lupeol - RM561940 / RM563558 / RM564495

   Dados ficticios, suficientes para o SELECT inicial do video nao sair
   vazio. So insere quando a tabela esta vazia: reexecutar nao duplica.
   Salvo em UTF-8; o 04_sql-schema.sh chama o sqlcmd com -f 65001.
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.cliente)
BEGIN
    INSERT INTO dbo.cliente (nome, cpf, email) VALUES
        (N'Ana Souza',  N'11122233344', N'ana.souza@exemplo.com'),
        (N'Bruno Lima', N'55566677788', N'bruno.lima@exemplo.com');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.transacao)
BEGIN
    INSERT INTO dbo.transacao (id_cliente, descricao, valor, tipo)
    SELECT id_cliente, N'Depósito inicial', 1500.00, N'CREDITO'
      FROM dbo.cliente WHERE cpf = N'11122233344'
    UNION ALL
    SELECT id_cliente, N'Salário de setembro', 4200.00, N'CREDITO'
      FROM dbo.cliente WHERE cpf = N'55566677788'
    UNION ALL
    SELECT id_cliente, N'Compra no supermercado', 250.75, N'DEBITO'
      FROM dbo.cliente WHERE cpf = N'55566677788';
END;
GO
