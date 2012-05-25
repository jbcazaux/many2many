R�cemment j'ai refait un peu de<strong> JPA/Hibernate</strong> et j'ai eu besoin de mapper une relation <strong>many-to-many</strong> qui n�cessite de stocker une information suppl�mentaire. Pour �tre concret, j'ai un objet <strong>Recette</strong>, un objet <strong>Ingredient </strong>et je souhaite <strong>associer une recette � un ingr�dient</strong>. Ce qui est particulier dans mon cas c'est que j'ai envie de d'enregistrer en plus la <strong>quantit� </strong>de cet ingr�dient qui est utilis� dans ma recette de cuisine.

Rien de bien extraordinaire jusque l� et je me souvenais avoir d�j� g�r� ce genre de cas. Oui mais voil�, apr�s quelques recherches sur les forums je n'ai trouv� aucune solution cl� en main qui ne n�cessite pas de parcourir les dizaines de commentaires et de les tester un � un.

Voici la solution que j'ai retenue, je suis bien sur ouvert � toute proposition permettant d'am�liorer mon code ! Je ne pr�senterai ici que les mappings, je vous propose de retrouver l'int�gralit� des cas d'utilisations sur mon github (avec les Tests Unitaires et surtout le debug hibernate pour voir les requ�tes qui passent).

L'id�e g�n�rale est de d�couper le many to many en deux relations many-to-one/one-to-many avec l'utilsation d'un <strong>objet d'association</strong> qui va porter l'information que l'on souhaite rajouter (ici la quantit�).

<strong>NB</strong>: je me sers de <a href="http://projectlombok.org/">lombok </a> pour g�n�rer les getter, setter et equals/hashcode de mes classes.

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

Les "astuces" que j'ai mis un moment � comprendre sont:
<ul>
	<li>dans la classe RecipeIngredientId il faut absolument mettre les relations en <strong>LAZY </strong>pour �viter un stackOverFlow;</li>

	<li> dans la classe Recipe, la relation OneToMany doit absolument comporter le <strong>orphanRemoval = true</strong>, sinon la mise � jour et la suppression des RecipeIngredient se passent mal !</li>
</ul>


Bon tout n'est pas parfait et il faut g�rer quelques trucs "� la main".
Il faut n�cessairement persister en base explicitement l'ingr�dient que l'on veut utiliser dans une recette que l'on souhaite elle-m�me persister.

<pre lang="java">
    /**
     * Parcourt l'ensemble des ingr�dient pour voir si ils existe d�j� en base ou non.
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

Il faut �crire une requ�te JPQL pour r�cup�rer la Recette dans son int�gralit�, si on veut par exemple la serialiser apr�s (certaines relations avaient �t� mises en LAZY, notamment sur la PK).
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
