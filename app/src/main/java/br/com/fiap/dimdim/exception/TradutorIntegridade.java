package br.com.fiap.dimdim.exception;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

/**
 * Converte a violacao de integridade do SQL Server numa mensagem legivel.
 *
 * O SQL Server cita o nome da constraint no texto do erro (547 para FK e
 * CHECK, 2627 para UNIQUE). Como todas as constraints do scripts/DDL.sql tem
 * nome proprio, basta procurar o nome na mensagem da causa mais especifica.
 */
public final class TradutorIntegridade {

    private static final String MENSAGEM_PADRAO = "A operação viola uma regra de integridade do banco de dados.";

    private static final Map<String, String> MENSAGENS_POR_CONSTRAINT = Map.of(
            "uk_cliente_cpf", "Já existe um cliente cadastrado com este CPF.",
            "ck_cliente_cpf", "O CPF deve conter exatamente 11 dígitos numéricos.",
            "fk_transacao_cliente", "Não é possível excluir o cliente: existem transações vinculadas a ele. "
                    + "Exclua primeiro as transações do cliente.",
            "ck_transacao_tipo", "Tipo de transação inválido. Valores aceitos: CREDITO ou DEBITO.",
            "ck_transacao_valor", "O valor da transação deve ser maior que zero."
    );

    private TradutorIntegridade() {
    }

    public static String traduzir(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause().getMessage();
        if (causa == null) {
            return MENSAGEM_PADRAO;
        }
        return MENSAGENS_POR_CONSTRAINT.entrySet().stream()
                .filter(par -> causa.contains(par.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(MENSAGEM_PADRAO);
    }
}
