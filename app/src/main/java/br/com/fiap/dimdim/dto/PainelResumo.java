package br.com.fiap.dimdim.dto;

import java.math.BigDecimal;

public record PainelResumo(long totalClientes, long totalTransacoes, BigDecimal saldoGeral) {
}
