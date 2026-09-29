/* =====================================================================
   Projeto DimDim - 2o Checkpoint: Web App + Azure SQL
   FIAP - DevOps Tools & Cloud Computing

   Grupo lupeol
     RM561940 - Pedro Franca   (representante)
     RM563558 - Olavo Neves
     RM564495 - Luiz Goncalves

   SGBD   : Azure SQL Database (T-SQL)
   Banco  : db_dimdim
   Modelo : cliente 1:N transacao

   Idempotente: cada tabela so e criada se ainda nao existir. Aplicado por
   scripts/04_sql-schema.sh com o login administrador do servidor.
   ===================================================================== */

/* ---------------------------------------------------------------------
   Tabela: cliente
   Cliente do banco digital DimDim.

   Todo texto e NVARCHAR (Unicode): nomes com acento sao gravados sem perda.
   data_cadastro e preenchida pelo banco no horario de Brasilia: o Azure SQL
   roda em UTC, e sem a conversao a tela mostraria 3 horas a mais.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.cliente', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cliente (
        id_cliente    BIGINT        IDENTITY(1,1) NOT NULL,
        nome          NVARCHAR(120) NOT NULL,
        cpf           NVARCHAR(11)  NOT NULL,
        email         NVARCHAR(150) NOT NULL,
        data_cadastro DATETIME2(0)  NOT NULL
            CONSTRAINT df_cliente_data_cadastro
            DEFAULT CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time' AS DATETIME2(0)),

        CONSTRAINT pk_cliente     PRIMARY KEY (id_cliente),
        CONSTRAINT uk_cliente_cpf UNIQUE (cpf),
        -- Mesma regra do DTO (\d{11}), repetida no banco: o Query Editor
        -- tambem escreve, e a API nao e o unico caminho de gravacao.
        CONSTRAINT ck_cliente_cpf CHECK (LEN(cpf) = 11 AND cpf NOT LIKE N'%[^0-9]%')
    );

    EXEC sp_addextendedproperty N'MS_Description', N'Cliente do banco digital DimDim',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente';
    EXEC sp_addextendedproperty N'MS_Description', N'Identificador do cliente (PK, gerado pelo banco)',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente', N'COLUMN', N'id_cliente';
    EXEC sp_addextendedproperty N'MS_Description', N'Nome completo',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente', N'COLUMN', N'nome';
    EXEC sp_addextendedproperty N'MS_Description', N'CPF com 11 digitos, sem pontuacao (unico)',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente', N'COLUMN', N'cpf';
    EXEC sp_addextendedproperty N'MS_Description', N'E-mail de contato',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente', N'COLUMN', N'email';
    EXEC sp_addextendedproperty N'MS_Description', N'Data e hora do cadastro (horario de Brasilia, gerada pelo banco)',
        N'SCHEMA', N'dbo', N'TABLE', N'cliente', N'COLUMN', N'data_cadastro';
END;
GO

/* ---------------------------------------------------------------------
   Tabela: transacao
   Movimentacao financeira de um cliente (credito ou debito).

   A FK usa ON DELETE NO ACTION (o RESTRICT do T-SQL): apagar um cliente que
   possui transacoes falha no banco, e a aplicacao devolve 409 / aviso na
   tela. E preciso apagar antes as transacoes do cliente.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.transacao', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.transacao (
        id_transacao   BIGINT        IDENTITY(1,1) NOT NULL,
        id_cliente     BIGINT        NOT NULL,
        descricao      NVARCHAR(200) NOT NULL,
        valor          DECIMAL(15,2) NOT NULL,
        tipo           NVARCHAR(10)  NOT NULL,
        data_transacao DATETIME2(0)  NOT NULL
            CONSTRAINT df_transacao_data
            DEFAULT CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'E. South America Standard Time' AS DATETIME2(0)),

        CONSTRAINT pk_transacao        PRIMARY KEY (id_transacao),
        CONSTRAINT ck_transacao_tipo   CHECK (tipo IN (N'CREDITO', N'DEBITO')),
        CONSTRAINT ck_transacao_valor  CHECK (valor > 0),
        CONSTRAINT fk_transacao_cliente
            FOREIGN KEY (id_cliente)
            REFERENCES dbo.cliente (id_cliente)
            ON DELETE NO ACTION
    );

    EXEC sp_addextendedproperty N'MS_Description', N'Transacao financeira de um cliente DimDim',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao';
    EXEC sp_addextendedproperty N'MS_Description', N'Identificador da transacao (PK, gerado pelo banco)',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'id_transacao';
    EXEC sp_addextendedproperty N'MS_Description', N'Cliente dono da transacao (FK para cliente)',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'id_cliente';
    EXEC sp_addextendedproperty N'MS_Description', N'Descricao livre da movimentacao',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'descricao';
    EXEC sp_addextendedproperty N'MS_Description', N'Valor positivo com 2 casas decimais',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'valor';
    EXEC sp_addextendedproperty N'MS_Description', N'CREDITO ou DEBITO',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'tipo';
    EXEC sp_addextendedproperty N'MS_Description', N'Data e hora da transacao (horario de Brasilia, gerada pelo banco)',
        N'SCHEMA', N'dbo', N'TABLE', N'transacao', N'COLUMN', N'data_transacao';
END;
GO

/* ---------------------------------------------------------------------
   Indice da FK
   O SQL Server NAO cria indice automatico para FK (diferente do InnoDB).
   Sem ele, excluir um cliente varre a tabela transacao inteira.
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = N'ix_transacao_id_cliente'
                 AND object_id = OBJECT_ID(N'dbo.transacao'))
BEGIN
    CREATE INDEX ix_transacao_id_cliente ON dbo.transacao (id_cliente);
END;
GO
