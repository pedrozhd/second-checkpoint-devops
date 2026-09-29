package br.com.fiap.dimdim.repository;

import br.com.fiap.dimdim.entity.Cliente;

import java.util.List;

public interface ClienteRepository extends RepositorioBase<Cliente, Long> {

    List<Cliente> findAllByOrderByNomeAsc();
}
