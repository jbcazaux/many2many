package fr.valtech.many2many.domain;

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recipe_ingredient")
@AssociationOverrides({
        @AssociationOverride(name = "pk.recipe", joinColumns = @JoinColumn(name = "recipe_id", insertable = false, updatable = false)),
        @AssociationOverride(name = "pk.ingredient", joinColumns = @JoinColumn(name = "ingredient_id", insertable = false, updatable = false)) })
@EqualsAndHashCode(of = { "pk", "amount" }, callSuper = false)
public class RecipeIngredient implements Serializable {

    @Getter
    @Setter
    @Column(nullable = false)
    private String amount;

    @Getter
    @Setter
    @EmbeddedId
    private RecipeIngredientId pk = new RecipeIngredientId();

    @Transient
    public Recipe getRecipe() {
        return getPk().getRecipe();
    }

    public void setRecipe(Recipe recipe) {
        getPk().setRecipe(recipe);
    }

    @Transient
    public Ingredient getIngredient() {
        return getPk().getIngredient();
    }

    public void setIngredient(Ingredient ingredient) {
        getPk().setIngredient(ingredient);
    }

    public RecipeIngredient() {
    }

    public RecipeIngredient(Ingredient ingredient, String amount) {
        setIngredient(ingredient);
        this.amount = amount;
    }

}
