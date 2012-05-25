package fr.valtech.many2many.dao;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractDAO implements DataAccessObject {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected Logger getLogger() {
        return logger;
    }

    @Override
    public <T> T save(final T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Override
    public <T> T merge(final T entity) {
        entityManager.merge(entity);
        return entity;
    }

    @Override
    public <T> T getEntity(Class<T> entityClass, Serializable id) {
        T entity = getEntityManager().find(entityClass, id);
        if (entity == null) {
            StringBuilder sb = new StringBuilder("entity not found (class=");
            sb.append(entityClass);
            sb.append(", id=");
            sb.append(id);
            sb.append(')');
            getLogger().info(sb.toString());
        }
        return entity;
    }
}
