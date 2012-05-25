package fr.valtech.many2many.dao;

import java.util.List;

import fr.valtech.many2many.domain.Ingredient;

public interface IngredientDAO extends DataAccessObject {

    public List<Ingredient> findAll();

    public Ingredient findByLabel(String label);

}
