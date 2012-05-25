package fr.valtech.many2many.dao;

import java.util.Iterator;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Repository;

import fr.valtech.many2many.domain.Ingredient;
import fr.valtech.many2many.domain.Recipe;
import fr.valtech.many2many.domain.RecipeIngredient;

@Repository
public class RecipeDAOImpl extends AbstractDAO implements RecipeDAO {

    public Recipe merge(Recipe recipe) {

        reatachIngredients(recipe);
        getEntityManager().merge(recipe);
        return recipe;
    }

    public Recipe save(Recipe recipe) {

        reatachIngredients(recipe);
        getEntityManager().persist(recipe);
        return recipe;
    }

    /**
     * Parcourt l'ensemble des ingrédient pour voir si ils existe déjà en base
     * ou non.
     * 
     * @param recipe
     */
    private void reatachIngredients(Recipe recipe) {

        for (Iterator<RecipeIngredient> it = recipe.getRecipeIngredients()
                .iterator(); it.hasNext();) {
            RecipeIngredient ri = it.next();
            Ingredient ingredient = ri.getIngredient();
            if (ingredient.getId() != null && ingredient.getId() != 0) {
                Ingredient reference = getEntityManager().getReference(
                        Ingredient.class, ingredient.getId());
                ri.setIngredient(reference);
            } else {
                getEntityManager().persist(ingredient);
            }
        }
    }

    @Override
    public Recipe getLastRecipe() {
        Query q = getEntityManager().createQuery("select max (id) from Recipe");

        Recipe recipe = null;
        try {
            Integer id = (Integer) q.getSingleResult();
            recipe = this.getRecipeById(id);
        } catch (NoResultException nre) {
            getLogger().info("no result found");
        }
        return recipe;
    }

    public Recipe getRecipeById(Integer recipeId) {
        Query q = getEntityManager().createQuery(
                "select r from Recipe as r "
                        + "left join fetch r.recipeIngredients as ri "
                        + "left join fetch ri.pk as pk "
                        + "left join fetch pk.ingredient "
                        + "where r.id = :recipeId");

        q.setParameter("recipeId", recipeId);

        Recipe recipe = null;
        try {
            recipe = (Recipe) q.getSingleResult();
        } catch (NoResultException nre) {
            getLogger().info("no result found");
            return null;
        }
        return initializeAndUnproxyRecipe(recipe);
    }

    private Recipe initializeAndUnproxyRecipe(Recipe recipe) {

        recipe = initializeAndUnproxy(recipe);
        for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
            ri.setIngredient(initializeAndUnproxy(ri.getPk().getIngredient()));
        }

        return recipe;
    }

    private static <T> T initializeAndUnproxy(T var) {
        if (var == null) {
            throw new IllegalArgumentException("passed argument is null");
        }

        Hibernate.initialize(var);
        if (var instanceof HibernateProxy) {
            var = (T) ((HibernateProxy) var).getHibernateLazyInitializer()
                    .getImplementation();
        }
        return var;
    }

}
