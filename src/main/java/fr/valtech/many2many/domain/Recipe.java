package fr.valtech.many2many.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recipe")
@EqualsAndHashCode(of = { "id", "title" }, callSuper = false)
public class Recipe extends AbstractEntity {

    @Id
    @Column(name = "recipe_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "pk.recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipeIngredient> recipeIngredients = new HashSet<RecipeIngredient>();

    public void addRecipeIngredient(RecipeIngredient i) {
        i.setRecipe(this);
        recipeIngredients.add(i);
    }
}
