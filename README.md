Récemment j'ai refait un peu de<strong> JPA/Hibernate</strong> et j'ai eu besoin de mapper une relation <strong>many-to-many</strong> qui nécessite de stocker une information supplémentaire. Pour être concret, j'ai un objet <strong>Recette</strong>, un objet <strong>Ingredient </strong>et je souhaite <strong>associer une recette à un ingrédient</strong>. Ce qui est particulier dans mon cas c'est que j'ai envie de d'enregistrer en plus la <strong>quantité </strong>de cet ingrédient qui est utilisé dans ma recette de cuisine.

Rien de bien extraordinaire jusque là et je me souvenais avoir déjà géré ce genre de cas. Oui mais voilà, après quelques recherches sur les forums je n'ai trouvé aucune solution clé en main qui ne nécessite pas de parcourir les dizaines de commentaires et de les tester un à un.

Voici la solution que j'ai retenue, je suis bien sur ouvert à toute proposition permettant d'améliorer mon code ! Je ne présenterai ici que les mappings, je vous propose de retrouver l'intégralité des cas d'utilisations sur mon github (avec les Tests Unitaires et surtout le debug hibernate pour voir les requêtes qui passent).

L'idée générale est de découper le many to many en deux relations many-to-one/one-to-many avec l'utilsation d'un <strong>objet d'association</strong> qui va porter l'information que l'on souhaite rajouter (ici la quantité).

<strong>NB</strong>: je me sers de <a href="http://projectlombok.org/">lombok </a> pour générer les getter, setter et equals/hashcode de mes classes.

La classe Recipe:
<pre lang="java">
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
  private Set ingredients = new HashSet();

  public void addIngredient(RecipeIngredient i) {
    i.setRecipe(this);
    ingredients.add(i);
  }
}
</pre>

La classe Ingredient:
<pre lang="java">
package fr.valtech.many2many.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@EqualsAndHashCode(of = { "id", "label" }, callSuper = false)
public class Ingredient extends AbstractEntity {

    @Id
    @Column(name = "ingredient_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private String label;

}
</pre>

La classe RecipeIngredient qui fait l'association:
<pre lang="java">
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

</pre>

Et enfin la composite primary key de la classe d'association:

<pre lang="java">

package fr.valtech.many2many.domain;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@EqualsAndHashCode(of = { "recipe", "ingredient" }, callSuper = false)
public class RecipeIngredientId implements Serializable {

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Recipe recipe;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Ingredient ingredient;

}

</pre>

Les "astuces" que j'ai mis un moment à comprendre sont:
<ul>
	<li>dans la classe RecipeIngredientId il faut absolument mettre les relations en <strong>LAZY </strong>pour éviter un stackOverFlow;</li>

	<li> dans la classe Recipe, la relation OneToMany doit absolument comporter le <strong>orphanRemoval = true</strong>, sinon la mise à jour et la suppression des RecipeIngredient se passent mal !</li>
</ul>


Bon tout n'est pas parfait et il faut gérer quelques trucs "à la main".
Il faut nécessairement persister en base explicitement l'ingrédient que l'on veut utiliser dans une recette que l'on souhaite elle-même persister.

<pre lang="java">
    /**
     * Parcourt l'ensemble des ingrédient pour voir si ils existe déjà en base ou non.
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
</pre>

Il faut écrire une requête JPQL pour récupérer la Recette dans son intégralité, si on veut par exemple la serialiser après (certaines relations avaient été mises en LAZY, notamment sur la PK).
<pre lang="java">
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
</pre>
