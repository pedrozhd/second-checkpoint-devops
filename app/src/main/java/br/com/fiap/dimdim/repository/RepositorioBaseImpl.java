package br.com.fiap.dimdim.repository;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao de RepositorioBase, registrada em JpaConfig via
 * repositoryBaseClass.
 */
public class RepositorioBaseImpl<T, ID> extends SimpleJpaRepository<T, ID>
        implements RepositorioBase<T, ID> {

    private final EntityManager entityManager;

    public RepositorioBaseImpl(JpaEntityInformation<T, ?> entityInformation,
                               EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void refresh(T entidade) {
        entityManager.refresh(entidade);
    }
}
