package br.com.fiap.dimdim.service;

import br.com.fiap.dimdim.dto.ClienteRequest;
import br.com.fiap.dimdim.dto.ClienteResponse;
import br.com.fiap.dimdim.entity.Cliente;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.exception.TradutorIntegridade;
import br.com.fiap.dimdim.repository.ClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regra de negocio de cliente, compartilhada pela API e pelas telas.
 */
@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAllByOrderByNomeAsc().stream()
                .map(ClienteResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = new Cliente();
        aplicar(cliente, request);
        Cliente salvo = gravar(cliente);
        // data_cadastro vem do DEFAULT do banco: recarrega para devolve-la.
        repository.refresh(salvo);
        return ClienteResponse.de(salvo);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        aplicar(cliente, request);
        return ClienteResponse.de(gravar(cliente));
    }

    /**
     * A FK transacao -> cliente e ON DELETE NO ACTION: apagar um cliente que
     * possua transacoes falha no banco, e a falha vira RegraIntegridadeException.
     */
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidade(id);
        try {
            repository.delete(cliente);
            // flush agora, para a violacao acontecer aqui e nao no commit.
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RegraIntegridadeException(TradutorIntegridade.traduzir(ex));
        }
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    private Cliente buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para o id " + id));
    }

    private Cliente gravar(Cliente cliente) {
        try {
            // saveAndFlush: CPF duplicado estoura aqui, dentro do try.
            return repository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraIntegridadeException(TradutorIntegridade.traduzir(ex));
        }
    }

    private static void aplicar(Cliente cliente, ClienteRequest request) {
        cliente.setNome(request.getNome());
        cliente.setCpf(request.getCpf());
        cliente.setEmail(request.getEmail());
    }
}
