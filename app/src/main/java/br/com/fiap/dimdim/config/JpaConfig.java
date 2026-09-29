package br.com.fiap.dimdim.config;

import br.com.fiap.dimdim.repository.RepositorioBaseImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registra o RepositorioBaseImpl como classe base dos repositorios.
 *
 * Fica fora da DimdimApplication de proposito: os testes @WebMvcTest nao
 * carregam classes @Configuration encontradas por component scan, mas
 * carregariam qualquer @EnableJpaRepositories declarado na classe principal
 * e tentariam subir o JPA sem banco.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "br.com.fiap.dimdim.repository",
        repositoryBaseClass = RepositorioBaseImpl.class
)
public class JpaConfig {
}
