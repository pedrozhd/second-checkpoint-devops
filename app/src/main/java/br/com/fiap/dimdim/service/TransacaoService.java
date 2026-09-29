package br.com.fiap.dimdim.service;

import br.com.fiap.dimdim.dto.TransacaoRequest;
import br.com.fiap.dimdim.dto.TransacaoResponse;
import br.com.fiap.dimdim.entity.Cliente;
import br.com.fiap.dimdim.entity.Transacao;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.exception.TradutorIntegridade;
import br.com.fiap.dimdim.repository.ClienteRepository;
import br.com.fiap.dimdim.repository.TransacaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regra de negocio de transacao, compartilhada pela API e pelas telas.
 * Todo DTO e montado dentro da transacao, porque o cliente e LAZY.
 */
@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ClienteRepository clienteRepository;

    public TransacaoService(TransacaoRepository transacaoRepository, ClienteRepository clienteRepository) {
        this.transacaoRepository = transacaoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listar() {
        return transacaoRepository.listarComCliente().stream()
                .map(TransacaoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransacaoResponse buscar(Long id) {
        return TransacaoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public TransacaoResponse criar(TransacaoRequest request) {
        Transacao transacao = new Transacao();
        aplicar(transacao, request);
        Transacao salva = gravar(transacao);
        // data_transacao vem do DEFAULT do banco: recarrega para devolve-la.
        transacaoRepository.refresh(salva);
        return TransacaoResponse.de(salva);
    }

    @Transactional
    public TransacaoResponse atualizar(Long id, TransacaoRequest request) {
        Transacao transacao = buscarEntidade(id);
        aplicar(transacao, request);
        return TransacaoResponse.de(gravar(transacao));
    }

    @Transactional
    public void excluir(Long id) {
        transacaoRepository.delete(buscarEntidade(id));
        transacaoRepository.flush();
    }

    @Transactional(readOnly = true)
    public long contar() {
        return transacaoRepository.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoGeral() {
        return transacaoRepository.calcularSaldoGeral();
    }

    private Transacao buscarEntidade(Long id) {
        return transacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada para o id " + id));
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para o id " + idCliente));
    }

    private Transacao gravar(Transacao transacao) {
        try {
            return transacaoRepository.saveAndFlush(transacao);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraIntegridadeException(TradutorIntegridade.traduzir(ex));
        }
    }

    private void aplicar(Transacao transacao, TransacaoRequest request) {
        transacao.setCliente(buscarCliente(request.getIdCliente()));
        transacao.setDescricao(request.getDescricao());
        transacao.setValor(request.getValor());
        transacao.setTipo(request.getTipo());
    }
}
