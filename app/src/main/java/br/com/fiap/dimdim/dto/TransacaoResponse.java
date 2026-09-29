package br.com.fiap.dimdim.dto;

import br.com.fiap.dimdim.entity.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponse(
        Long idTransacao,
        Long idCliente,
        String nomeCliente,
        String descricao,
        BigDecimal valor,
        String tipo,
        LocalDateTime dataTransacao
) {

    public static TransacaoResponse de(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getIdTransacao(),
                transacao.getCliente().getIdCliente(),
                transacao.getCliente().getNome(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getTipo(),
                transacao.getDataTransacao()
        );
    }
}
