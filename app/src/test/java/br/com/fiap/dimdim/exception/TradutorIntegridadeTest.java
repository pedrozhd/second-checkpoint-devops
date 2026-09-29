package br.com.fiap.dimdim.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class TradutorIntegridadeTest {

    // O SQL Server devolve o nome da constraint no texto do erro; o Spring
    // embrulha a SQLException numa DataIntegrityViolationException.
    private static DataIntegrityViolationException violacao(String mensagemDoBanco) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException(mensagemDoBanco));
    }

    @Test
    void traduzir_cpfDuplicado_retornaMensagemDeCpf() {
        String mensagem = TradutorIntegridade.traduzir(violacao(
                "Violation of UNIQUE KEY constraint 'uk_cliente_cpf'. Cannot insert duplicate key in object 'dbo.cliente'."));

        assertThat(mensagem).isEqualTo("Já existe um cliente cadastrado com este CPF.");
    }

    @Test
    void traduzir_clienteComTransacoes_retornaMensagemDeExclusao() {
        String mensagem = TradutorIntegridade.traduzir(violacao(
                "The DELETE statement conflicted with the REFERENCE constraint \"fk_transacao_cliente\". "
                        + "The conflict occurred in database \"db_dimdim\", table \"dbo.transacao\", column 'id_cliente'."));

        assertThat(mensagem).isEqualTo("Não é possível excluir o cliente: existem transações vinculadas a ele. "
                + "Exclua primeiro as transações do cliente.");
    }

    @Test
    void traduzir_tipoInvalido_retornaMensagemDeTipo() {
        String mensagem = TradutorIntegridade.traduzir(violacao(
                "The INSERT statement conflicted with the CHECK constraint \"ck_transacao_tipo\"."));

        assertThat(mensagem).isEqualTo("Tipo de transação inválido. Valores aceitos: CREDITO ou DEBITO.");
    }

    @Test
    void traduzir_valorNaoPositivo_retornaMensagemDeValor() {
        String mensagem = TradutorIntegridade.traduzir(violacao(
                "The INSERT statement conflicted with the CHECK constraint \"ck_transacao_valor\"."));

        assertThat(mensagem).isEqualTo("O valor da transação deve ser maior que zero.");
    }

    @Test
    void traduzir_cpfComLetras_retornaMensagemDeFormatoDoCpf() {
        String mensagem = TradutorIntegridade.traduzir(violacao(
                "The UPDATE statement conflicted with the CHECK constraint \"ck_cliente_cpf\"."));

        assertThat(mensagem).isEqualTo("O CPF deve conter exatamente 11 dígitos numéricos.");
    }

    @Test
    void traduzir_constraintDesconhecida_retornaMensagemGenerica() {
        String mensagem = TradutorIntegridade.traduzir(violacao("Some other database error."));

        assertThat(mensagem).isEqualTo("A operação viola uma regra de integridade do banco de dados.");
    }
}
