package br.com.fiap.dimdim.repository;

import br.com.fiap.dimdim.entity.Transacao;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface TransacaoRepository extends RepositorioBase<Transacao, Long> {

    // join fetch: a listagem mostra o nome do cliente sem um SELECT por linha.
    @Query("select t from Transacao t join fetch t.cliente order by t.dataTransacao desc, t.idTransacao desc")
    List<Transacao> listarComCliente();

    @Query("select coalesce(sum(case when t.tipo = 'CREDITO' then t.valor else -t.valor end), 0) from Transacao t")
    BigDecimal calcularSaldoGeral();
}
