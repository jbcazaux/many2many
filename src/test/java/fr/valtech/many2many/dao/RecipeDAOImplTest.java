package fr.valtech.many2many.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import junit.framework.Assert;

import org.hibernate.proxy.HibernateProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import fr.valtech.many2many.domain.Ingredient;
import fr.valtech.many2many.domain.Recipe;
import fr.valtech.many2many.domain.RecipeIngredient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/many2many.xml",
        "classpath:/datasource-test.xml" })
@Transactional
public class RecipeDAOImplTest {

    @Autowired
    private RecipeDAO recipeDAO;

    @PersistenceContext
    protected EntityManager entityManager;

    private Recipe recipe;
    private Ingredient jambon;
    private RecipeIngredient _400gJambon;

    protected void flushSession() throws Exception {
        entityManager.flush();
    }

    @Before
    public void setUp() throws Exception {

        jambon = new Ingredient();
        jambon.setLabel("jambon");

        _400gJambon = new RecipeIngredient();
        _400gJambon.setAmount("400g grammes");
        _400gJambon.setIngredient(jambon);

        recipe = new Recipe();
        recipe.setTitle("title");
        recipe.addRecipeIngredient(_400gJambon);

    }

    @Test
    public void testSaveAndLoad() throws Exception {

        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        flushSession();

        Recipe searchedRecipe = recipeDAO.getEntity(Recipe.class,
                recipe.getId());
        assertEquals(recipe.getId(), searchedRecipe.getId());
        assertEquals(jambon.getLabel(), searchedRecipe.getRecipeIngredients()
                .iterator().next().getIngredient().getLabel());
    }

    @Test
    public void testCreate2recipesWithSameIngredients() throws Exception {

        // recette1
        Ingredient pain = new Ingredient();
        pain.setLabel("pain");

        RecipeIngredient _100gPain = new RecipeIngredient(pain, "100g");
        recipe.addRecipeIngredient(_100gPain);
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        flushSession();

        // recette2
        Recipe recipe2 = new Recipe();
        recipe2.setTitle("title2");
        Iterator<RecipeIngredient> it = recipe.getRecipeIngredients()
                .iterator();

        // les ingredients détachés
        Ingredient i1 = new Ingredient();
        i1.setId(it.next().getIngredient().getId());
        RecipeIngredient ri1 = new RecipeIngredient(i1, "200g");
        recipe2.addRecipeIngredient(ri1);

        Ingredient i2 = new Ingredient();
        i2.setId(it.next().getIngredient().getId());
        RecipeIngredient ri2 = new RecipeIngredient(i2, "300g");
        recipe2.addRecipeIngredient(ri2);

        assertFalse(i1.getId().equals(i2.getId()));
        assertFalse(i1.equals(i2));

        recipeDAO.save(recipe2);
        assertEquals(2, recipe2.getRecipeIngredients().size());
        flushSession();

        // asserts
        Recipe searchedRecipe2 = recipeDAO.getEntity(Recipe.class,
                recipe2.getId());
        Iterator<RecipeIngredient> itRecipe1 = recipe.getRecipeIngredients()
                .iterator();
        i1 = itRecipe1.next().getIngredient();
        i2 = itRecipe1.next().getIngredient();

        it = searchedRecipe2.getRecipeIngredients().iterator();
        boolean foundi1 = false;
        boolean foundi2 = false;
        while (it.hasNext()) {
            RecipeIngredient ri = it.next();
            if (ri.getIngredient().equals(i1)) {
                foundi1 = true;
            }
            if (ri.getIngredient().equals(i2)) {
                foundi2 = true;
            }
        }
        Assert.assertTrue(foundi1);
        Assert.assertTrue(foundi2);
    }

    @Test
    public void testLoadNonExistingRecipe() throws Exception {

        Recipe searchedRecipe = recipeDAO.getEntity(Recipe.class, -1);
        assertNull(searchedRecipe);
    }

    @Test
    public void testSaveAndUpdateDetachedRecipeWithNewIngredient()
            throws Exception {
        // sauve recette1
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        Assert.assertEquals(1, recipe.getRecipeIngredients().size());
        flushSession();

        Recipe searchedRecipe;
        // clean la session pour detacher les objets
        entityManager.clear();
        Ingredient pates = new Ingredient();
        pates.setLabel("pates");

        RecipeIngredient _100gPates = new RecipeIngredient(pates, "200g");

        searchedRecipe = detachRecipe(recipe);
        searchedRecipe.addRecipeIngredient(_100gPates);
        recipeDAO.merge(searchedRecipe);
        flushSession();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(2, recipe.getRecipeIngredients().size());
    }

    @Test
    public void testUpdateDetachedRecipeWithNewDetachedIngredients()
            throws Exception {
        // enregistre la recette
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        flushSession();

        // clean de la session pour detacher les objets
        entityManager.clear();
        Recipe searchedRecipe = detachRecipe(recipe);
        // ajoute un nouvel ingredient (sans id)
        Ingredient pates = new Ingredient();
        pates.setLabel("pates");
        RecipeIngredient _100gPates = new RecipeIngredient(pates, "100g");

        searchedRecipe.addRecipeIngredient(_100gPates);
        recipeDAO.merge(searchedRecipe);
        flushSession();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(2, recipe.getRecipeIngredients().size());
    }

    @Test
    public void testUpdateDetachedRecipeByAddingExistingDetachedIngredients()
            throws Exception {
        // enregistre la recette
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        // cree un nouvel ingredient en base
        Ingredient salade = new Ingredient();
        salade.setLabel("salade");
        entityManager.persist(salade);
        flushSession();

        // clean de la session et de la recette
        entityManager.clear();
        Ingredient saladeDetached = new Ingredient();
        saladeDetached.setId(salade.getId());

        Recipe searchedRecipe = detachRecipe(recipe);
        // ajoute un nouvel ingredient (avec id)
        RecipeIngredient _1feuilleSalade = new RecipeIngredient(saladeDetached,
                "1 feuille");
        searchedRecipe.addRecipeIngredient(_1feuilleSalade);

        recipeDAO.merge(searchedRecipe);
        flushSession();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(2, recipe.getRecipeIngredients().size());
    }

    @Test
    public void testUpdateDetachedRecipeByReplacingIngredientWithAnExistingDetachedIngredients()
            throws Exception {
        // enregistre la recette
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        // cree un nouvel ingredient en base
        Ingredient salade = new Ingredient();
        salade.setLabel("salade");
        entityManager.persist(salade);
        flushSession();

        // clean de la session et de la recette
        entityManager.clear();
        Ingredient saladeDetached = new Ingredient();
        saladeDetached.setId(salade.getId());

        Recipe searchedRecipe = detachRecipe(recipe);
        // ajoute un nouvel ingredient (avec id)
        RecipeIngredient ri = searchedRecipe.getRecipeIngredients().iterator()
                .next();
        ri.setIngredient(saladeDetached);

        recipeDAO.merge(searchedRecipe);
        flushSession();
        entityManager.clear();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(1, recipe.getRecipeIngredients().size());
        Assert.assertEquals(saladeDetached.getId(), recipe
                .getRecipeIngredients().iterator().next().getIngredient()
                .getId());
    }

    @Test
    public void testUpdateAmountOfDetachedIngredient() throws Exception {
        // sauve recette1
        Assert.assertNull(recipe.getId());
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        Assert.assertEquals(1, recipe.getRecipeIngredients().size());
        flushSession();

        // clean la session pour detacher les objets
        entityManager.clear();

        Recipe searchedRecipe = detachRecipe(recipe);
        Iterator<RecipeIngredient> it = searchedRecipe.getRecipeIngredients()
                .iterator();
        RecipeIngredient ri1 = it.next();
        ri1.setAmount("new amount");

        recipeDAO.merge(searchedRecipe);
        flushSession();
        entityManager.clear();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(1, recipe.getRecipeIngredients().size());
        Assert.assertEquals("new amount", recipe.getRecipeIngredients()
                .iterator().next().getAmount());
    }

    @Test
    public void testUpdateRecipeByDeletingDetachedIngredients()
            throws Exception {
        // enregistre la recette avec 2 ingredients (salade et jambon)
        Assert.assertNull(recipe.getId());
        Ingredient salade = new Ingredient();
        salade.setLabel("salade");
        RecipeIngredient _1feuilleSalade = new RecipeIngredient();
        _1feuilleSalade.setIngredient(salade);
        _1feuilleSalade.setAmount("1 feuille");
        recipe.addRecipeIngredient(_1feuilleSalade);
        recipeDAO.save(recipe);
        Assert.assertTrue(recipe.getId() > 0);
        flushSession();
        entityManager.clear();

        // detache la recette a modifier
        Recipe searchedRecipe = detachRecipe(recipe);
        assertEquals(2, searchedRecipe.getRecipeIngredients().size());
        // supprime un ingredient
        Iterator<RecipeIngredient> it = searchedRecipe.getRecipeIngredients()
                .iterator();
        it.next();
        it.remove();
        assertEquals(1, searchedRecipe.getRecipeIngredients().size());
        recipeDAO.merge(searchedRecipe);
        flushSession();
        entityManager.clear();

        recipe = recipeDAO.getEntity(Recipe.class, searchedRecipe.getId());
        Assert.assertEquals(1, recipe.getRecipeIngredients().size());
    }

    @Test
    public void testGetRecipeById() throws Exception {
        // recette1
        recipeDAO.save(recipe);
        flushSession();
        entityManager.clear();

        Recipe fullRecipe = recipeDAO.getRecipeById(recipe.getId());
        entityManager.clear();

        assertEquals(recipe.getId(), fullRecipe.getId());
        for (RecipeIngredient ri : fullRecipe.getRecipeIngredients()) {
            assertFalse(ri.getPk().getIngredient() instanceof HibernateProxy);
        }
    }

    @Test
    public void testGetRecipeByUnknownId() throws Exception {

        Integer unknownId = new Integer(12345);
        Recipe notFoundRecipe = recipeDAO.getRecipeById(unknownId);
        entityManager.clear();

        assertNull(notFoundRecipe);

    }

    private Recipe detachRecipe(Recipe r) {

        Recipe detachedRecipe = new Recipe();
        detachedRecipe.setId(r.getId());
        detachedRecipe.setTitle(r.getTitle());

        Iterator<RecipeIngredient> it = r.getRecipeIngredients().iterator();
        while (it.hasNext()) {
            RecipeIngredient ri = it.next();
            Ingredient i = new Ingredient();
            i.setId(ri.getIngredient().getId());
            RecipeIngredient detachedRi = new RecipeIngredient(i,
                    ri.getAmount());
            detachedRecipe.addRecipeIngredient(detachedRi);
        }

        return detachedRecipe;

    }

}
