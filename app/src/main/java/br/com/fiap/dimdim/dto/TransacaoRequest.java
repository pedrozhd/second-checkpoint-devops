package br.com.fiap.dimdim.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dados de entrada de transacao, usados pela API e pelo formulario.
 * Classe JavaBean pelo mesmo motivo de ClienteRequest.
 */
public class TransacaoRequest {

    @NotNull(message = "Selecione o cliente da transação.")
    private Long idCliente;

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    private String descricao;

    // inclusive=false garante valor estritamente maior que zero
    @NotNull(message = "O valor é obrigatório.")
    @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 decimais.")
    private BigDecimal valor;

    @NotBlank(message = "O tipo é obrigatório.")
    @Pattern(regexp = "CREDITO|DEBITO", message = "O tipo deve ser CREDITO ou DEBITO.")
    private String tipo;

    public TransacaoRequest() {
    }

    public TransacaoRequest(Long idCliente, String descricao, BigDecimal valor, String tipo) {
        this.idCliente = idCliente;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
    }

    public static TransacaoRequest de(TransacaoResponse transacao) {
        return new TransacaoRequest(transacao.idCliente(), transacao.descricao(),
                transacao.valor(), transacao.tipo());
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
