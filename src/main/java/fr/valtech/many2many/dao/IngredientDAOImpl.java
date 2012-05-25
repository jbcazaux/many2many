package fr.valtech.many2many.dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import fr.valtech.many2many.domain.Ingredient;

@Repository
public class IngredientDAOImpl extends AbstractDAO implements IngredientDAO {

    public List<Ingredient> findAll() {

        TypedQuery<Ingredient> allIngredients = getEntityManager().createQuery(
                "from Ingredient", Ingredient.class);
        return allIngredients.getResultList();
    }

    public Ingredient findByLabel(String label) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        CriteriaQuery<Ingredient> criteriaQuery = cb
                .createQuery(Ingredient.class);
        Root<Ingredient> i = criteriaQuery.from(Ingredient.class);
        criteriaQuery.where(cb.like(i.<String> get("label"),
                cb.parameter(String.class, "label")));
        TypedQuery<Ingredient> tq = getEntityManager().createQuery(
                criteriaQuery);
        tq.setParameter("label", label);

        Ingredient ingredient = null;
        try {
            ingredient = tq.getSingleResult();
        } catch (NoResultException nre) {
            getLogger().info("no result found");
        }
        return ingredient;
    }
}
