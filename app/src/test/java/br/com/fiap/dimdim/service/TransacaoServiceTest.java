package br.com.fiap.dimdim.service;

import br.com.fiap.dimdim.dto.TransacaoRequest;
import br.com.fiap.dimdim.dto.TransacaoResponse;
import br.com.fiap.dimdim.entity.Cliente;
import br.com.fiap.dimdim.entity.Transacao;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.repository.ClienteRepository;
import br.com.fiap.dimdim.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private TransacaoService service;

    @Test
    void criar_clienteInexistente_lancaRecursoNaoEncontradoSemGravar() {
        when(clienteRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new TransacaoRequest(42L, "Teste", new BigDecimal("10.00"), "CREDITO")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado para o id 42");
        verify(transacaoRepository, never()).saveAndFlush(any(Transacao.class));
    }

    @Test
    void criar_dadosValidos_vinculaClienteERetornaNomeDoCliente() {
        Cliente ana = new Cliente();
        ana.setIdCliente(1L);
        ana.setNome("Ana Souza");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(transacaoRepository.saveAndFlush(any(Transacao.class))).thenAnswer(invocacao -> {
            Transacao transacao = invocacao.getArgument(0);
            transacao.setIdTransacao(10L);
            return transacao;
        });

        TransacaoResponse resposta = service.criar(
                new TransacaoRequest(1L, "Depósito inicial", new BigDecimal("1500.00"), "CREDITO"));

        verify(transacaoRepository).refresh(any(Transacao.class));
        assertThat(resposta.idTransacao()).isEqualTo(10L);
        assertThat(resposta.idCliente()).isEqualTo(1L);
        assertThat(resposta.nomeCliente()).isEqualTo("Ana Souza");
    }

    @Test
    void buscar_idInexistente_lancaRecursoNaoEncontrado() {
        when(transacaoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(7L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Transação não encontrada para o id 7");
    }
}
