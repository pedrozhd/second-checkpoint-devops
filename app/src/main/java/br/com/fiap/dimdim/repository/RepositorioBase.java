package br.com.fiap.dimdim.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Acrescenta refresh() ao contrato padrao do Spring Data.
 *
 * As colunas data_cadastro e data_transacao sao preenchidas pelo DEFAULT do
 * banco. Apos o INSERT a entidade em memoria ainda tem esses campos nulos,
 * entao e preciso reler a linha para devolve-los na resposta.
 */
@NoRepositoryBean
public interface RepositorioBase<T, ID> extends JpaRepository<T, ID> {

    void refresh(T entidade);
}
