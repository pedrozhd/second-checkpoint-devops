package br.com.fiap.dimdim.service;

import br.com.fiap.dimdim.dto.ClienteRequest;
import br.com.fiap.dimdim.dto.ClienteResponse;
import br.com.fiap.dimdim.entity.Cliente;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void buscar_idInexistente_lancaRecursoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado para o id 99");
    }

    @Test
    void criar_dadosValidos_gravaERecarregaParaTrazerDataDoBanco() {
        when(repository.saveAndFlush(any(Cliente.class))).thenAnswer(invocacao -> {
            Cliente cliente = invocacao.getArgument(0);
            cliente.setIdCliente(1L);
            return cliente;
        });

        ClienteResponse resposta = service.criar(new ClienteRequest("Ana Souza", "11122233344", "ana@exemplo.com"));

        verify(repository).refresh(any(Cliente.class));
        assertThat(resposta.idCliente()).isEqualTo(1L);
        assertThat(resposta.nome()).isEqualTo("Ana Souza");
    }

    @Test
    void criar_cpfDuplicado_lancaRegraIntegridadeComMensagemDeCpf() {
        when(repository.saveAndFlush(any(Cliente.class))).thenThrow(new DataIntegrityViolationException("x",
                new SQLException("Violation of UNIQUE KEY constraint 'uk_cliente_cpf'.")));

        assertThatThrownBy(() -> service.criar(new ClienteRequest("Ana Souza", "11122233344", "ana@exemplo.com")))
                .isInstanceOf(RegraIntegridadeException.class)
                .hasMessage("Já existe um cliente cadastrado com este CPF.");
    }

    @Test
    void excluir_clienteComTransacoes_lancaRegraIntegridade() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        doThrow(new DataIntegrityViolationException("x",
                new SQLException("The DELETE statement conflicted with the REFERENCE constraint \"fk_transacao_cliente\".")))
                .when(repository).flush();

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(RegraIntegridadeException.class)
                .hasMessageContaining("transações vinculadas");
    }
}
