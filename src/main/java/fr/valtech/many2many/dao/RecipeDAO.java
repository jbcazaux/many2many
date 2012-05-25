package fr.valtech.many2many.dao;

import fr.valtech.many2many.domain.Recipe;

public interface RecipeDAO extends DataAccessObject {

    public Recipe merge(Recipe recipe);

    public Recipe save(Recipe recipe);

    public Recipe getLastRecipe();

    public Recipe getRecipeById(Integer id);
}
