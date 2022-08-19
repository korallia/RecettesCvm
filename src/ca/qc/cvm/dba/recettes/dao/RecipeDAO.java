package ca.qc.cvm.dba.recettes.dao;

import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import ca.qc.cvm.dba.recettes.entity.Ingredient;
import ca.qc.cvm.dba.recettes.entity.Recipe;

public class RecipeDAO {
	static MongoDatabase connectionMongo = MongoConnection.getConnection();
	static MongoCollection<Document> collection = connectionMongo.getCollection("collection_recettes");
	static Database connectionBerkeley = BerkeleyConnection.getConnection();
	static List<String> stepList = Arrays.asList("Mélangez dûment les ingrédients.", "Hachez finement la viande.", 
			"Laissez la concoction aérer quelques instants.", "Prenez plusieurs pas de recul de la planche à découper.", 
			"Tombez la chemise.", "S'il reste du liquide, laissez macérer.", "Attendrissez la protéine avec un objet quelconque.", 
			"Mettez le plat au four sur la grille du milieu.", "Cuisez le plat au moins 8 heures dans votre Crock Pot à faible température, ou pendant quelque secondes sur du magma.",
			"Mettez au micro-onde le temps que ça cuise.", "Triez les aliments coupés du plus petit morceau au plus gros.", 
			"Passez par la case départ.", "Mettez au moins la moitié de la préparation au congélateur.", 
			"Contentez-vous du résultat s'il ne ressemble pas à l'image.", "Coupez 42 tranches sur le long.", "Testez l'étanchéité de votre plat.",
			"Si le plat n'a pas levé au four, abandonnez et commmandez du Valentine.", "Malaxez jusqu'à ce que ce soit assez.", "Saupoudrez très peu ou beaucoup trop de farine. Répétez.",
			"Remuez le mélange vigoureusement.", "Tendez l'autre joue.", "Prenez une pause, vous le méritez.",
			"Êtes-vous certain de vouloir poursuivre?", "Givrez avec une canette de Paris Pâté.", "Vérifiez si le mélange, à ce stade-ci, colle au mur si lancé.");
	
	static List<String> prefixList = Arrays.asList("Bisque de ", "Velouté de ", "Potée de ", "Pôélée de ", "Boulettes de ", "Gratin de", "Rillette de",
			"Verrine de ", "Bortsch de ", "Houmous de ", "Carpaccio de ", "Pain sandwhich de ", "Vari-Baril de ", "Bouillie de ", "Grillades de ",
			"Tartiflette de ", "Ragoût Ragoûtant de ", "Gyros de ", "Schnitzel de ", "Poke Bowl de " );
	
	/**
	 * M�thode permettant de sauvegarder une recette
	 * 
	 * Notes importantes:
	 * - Si le champ "id" n'est pas null, alors c'est une mise � jour, autrement c'est une insertion
	 * - Le nom de la recette doit �tre unique
	 * - Regarder comment est fait la classe Recette et Ingredient pour avoir une id�e des donn�es � sauvegarder
	 * - � l'insertion, il doit y avoir un id num�rique unique associ� � la recette. 
	 *   D�pendemment de la base de donn�es, vous devrez trouver une strat�gie pour faire un id num�rique.
	 * 
	 * @param recette
	 * @return true si succ�s, false sinon
	 */
	
	public static boolean save(Recipe recipe) {
		boolean success = false;
		Document doc = new Document();
		Document ingredients = new Document();
		
		try {
			for (int i = 0; i < recipe.getIngredients().size(); i++ ) {
				Document ingredient = new Document();
				ingredient.append("IngredientName",recipe.getIngredients().get(i).getName());
				ingredient.append("Quantity",recipe.getIngredients().get(i).getQuantity() );
				ingredients.append("Ingredient"+" "+(i+1), ingredient);
			}
			
			
			doc.append("Name", recipe.getName().toLowerCase());
			doc.append("Steps", recipe.getSteps());
			doc.append("Ingredients", ingredients);
			doc.append("Portion_Number", recipe.getPortion());
			doc.append("Cooktime", recipe.getCookTime());
			doc.append("Preptime", recipe.getPrepTime());
			
			if( recipe.getId()== null) {
				if( RecipeDAO.getRecipeCount()==0 ) {
					doc.append("ID",Integer.toUnsignedLong(1) );	
				}
				else {
					doc.append("ID", getLastAddedRecipe().getId()+1);
				}
				RecipeDAO.collection.insertOne(doc);
				DatabaseEntry theKey = new DatabaseEntry(recipe.getName().toLowerCase().getBytes("UTF-8"));
				DatabaseEntry theData = new DatabaseEntry(recipe.getImageData());
				RecipeDAO.connectionBerkeley.put(null, theKey, theData);
			}
			else {
				doc.append("ID", recipe.getId());
				Document IdDoc = new Document();
				IdDoc.append("ID", recipe.getId());
				RecipeDAO.collection.replaceOne(IdDoc, doc);
				DatabaseEntry theKey = new DatabaseEntry(recipe.getName().toLowerCase().getBytes("UTF-8"));
				DatabaseEntry theData = new DatabaseEntry(recipe.getImageData());
				RecipeDAO.connectionBerkeley.put(null, theKey, theData);
			}
			success = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return success;	
	}
		
	/**
	 * M�thode permettant de retourner la liste des recettes de la base de donn�es.
	 * 
	 * Notes importantes:
	 * - N'oubliez pas de limiter les r�sultats en fonction du param�tre limit
	 * - La liste doit �tre tri�es en ordre croissant, selon le nom des recettes
	 * -  (insensible � la casse)
	 * - N'oubliez pas de mettre l'ID dans la recette
	 * - Il pourrait ne pas y avoir de filtre (champ filtre vide)
	 * 	 * 
	 * @param filter champ filtre, peut �tre vide ou null
	 * @param limit permet de restreindre les r�sultats
	 * @return la liste des recettes, selon le filtre si n�cessaire 
	 */
	
	public static List<Recipe> getRecipeList(final String filter, int limit) {
		final List<Recipe> recipeList = new ArrayList<Recipe>();
		FindIterable<Document> iterator = null;
		
		if (filter.equals("")) {
		 iterator = collection.find();}
		else {
			Document regexQuerry = new Document();
			regexQuerry.append("$regex","^"+filter.toLowerCase() + ".*");
			Document criteria = new Document("Name",regexQuerry);
			iterator =collection.find(criteria);
		}
		iterator.limit(limit).sort(new Document("Name",1));
		
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					byte[] retData = null;
					Long Id = document.getLong("ID");
					Document ingredients = (Document) document.get("Ingredients");
					String recipeName = document.getString("Name");
					List<String> steps = (List<String>) document.get("Steps");
					int portions = document.getInteger("Portion_Number");
					int cookTime = document.getInteger("Cooktime");
					int prepTime = document.getInteger("Preptime");
					List<Ingredient> ingredientsList = new ArrayList<Ingredient>();
					for ( int i =0; i < ingredients.size();i++) {
						Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
						ingredientsList.add(new Ingredient(ingredient.getString("Quantity"),ingredient.getString("IngredientName")));
					}
					
					try {
						 DatabaseEntry theKey = new DatabaseEntry(recipeName.getBytes("UTF-8"));
						 DatabaseEntry theData = new DatabaseEntry();
						 if (connectionBerkeley.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) { 
						    	retData = theData.getData();
						    	recipeList.add(new Recipe(Id,recipeName,prepTime,cookTime,portions,steps,ingredientsList, retData));
						    } 
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();}}});
			} catch (Exception e) {
						e.printStackTrace();} 
		return recipeList;
		}
/**
	 * Suppression des donn�es li�es � une recette
	 * 
	 * @param recipe
	 * @return true si succ�s, false sinon
	 */
	
	public static boolean delete(Recipe recipe) {
		boolean success = false;
		collection.deleteOne(new Document("ID",recipe.getId()));
		
		try {
		    DatabaseEntry theKey = new DatabaseEntry(recipe.getName().getBytes("UTF-8"));
		    RecipeDAO.connectionBerkeley.delete(null, theKey);
		    success=true;
		    } 
		catch (Exception e) {
		    // Exception handling
			e.printStackTrace();}
		return success;
	}
	
	/**
	 * Suppression totale de toutes les donn�es du syst�me!
	 * 
	 * @return true si succ�s, false sinon
	 */
	
	public static boolean deleteAll() {
		boolean success = false;
		final List<String> keyList = new ArrayList<>();
		FindIterable<Document> iterator = collection.find();
		
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					String nom = document.getString("Name");
					keyList.add(nom);}
		   });
			collection.drop();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		try {	
			for (int i=0;i<keyList.size();i++) {
			    DatabaseEntry theKey = new DatabaseEntry(keyList.get(i).getBytes("UTF-8"));
			    RecipeDAO.connectionBerkeley.delete(null, theKey);
			}
				success = true;
		}catch (Exception e) {
				e.printStackTrace();
			}
		return success;
	}
	
	/**
	 * Permet de retourner le nombre d'ingr�dients en moyenne dans une recette
	 * 
	 * @return le nombre moyen d'ingr�dients
	 */
	public static double getAverageNumberOfIngredients() {
		final List<Ingredient> ingredientsList = new ArrayList<Ingredient>();
		
		FindIterable<Document> iterator = collection.find();

		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					Document ingredients = (Document) document.get("Ingredients");
					for ( int i =0; i < ingredients.size();i++) {
						Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
						ingredientsList.add(new Ingredient(ingredient.getString("Quantity"),ingredient.getString("IngredientName")));}
		   }});} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return ingredientsList.size()/getRecipeCount();
	}
	
	/**
	 * Permet d'obtenir le temps de la recette la plus longue � faire.
	 * 
	 * La recette la plus longue est calcul�e selon son temps de cuisson plus son temps de pr�paration
	 * 
	 * @return le temps maximal
	 */
	
	public static long getMaxRecipeTime() {
		final List<Integer> recipeTimeList = new ArrayList<>();
		
		FindIterable<Document> iterator = collection.find();
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					int cookTime = document.getInteger("Cooktime");
					int prepTime = document.getInteger("Preptime");
					recipeTimeList.add((cookTime+prepTime));	
		   }});} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.max(recipeTimeList);
	}
	
	/**
	 * Permet d'obtenir le nombre de photos dans la base de donn�es BerkeleyDB
	 * 
	 * @return nombre de photos dans BerkeleyDB
	 */
	
	public static long getPhotoCount() {
		long num = 0;
		Cursor myCursor = null;
		
		try {
		    myCursor = connectionBerkeley.openCursor(null, null);
		    DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();    
		    while(myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
		        num++;
		    }
		    myCursor.close();
		} 
		catch (DatabaseException e) {
			e.printStackTrace();
		} 
		return num;
	}

	/**
	 * Permet d'obtenir le nombre de recettes dans votre base de donn�es
	 * 
	 * @return nombre de recettes
	 */
	public static long getRecipeCount() {
		return RecipeDAO.collection.count();
	}
	
	/**
	 * Permet d'obtenir la derni�re recette ajout�e dans le syst�me
	 * 
	 * @return la derni�re recette
	 */
	
	public static Recipe getLastAddedRecipe() {
		final List<Recipe> recipeList = new ArrayList<Recipe>();
		final List<Ingredient> ingredientsList = new ArrayList<Ingredient>();
		FindIterable<Document> iterator = RecipeDAO.collection.find().sort(new Document("_id", -1)).limit(1);
		
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					byte[] retData = null;
					Long Id = document.getLong("ID");
					Document ingredients = (Document) document.get("Ingredients");
					String recipeName = document.getString("Name");
					List<String> steps = (List<String>) document.get("Steps");
					int portions = document.getInteger("Portion_Number");
					int cookTime = document.getInteger("Cooktime");
					int prepTime = document.getInteger("Preptime");
					
					for ( int i =0; i < ingredients.size();i++) {
						Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
						ingredientsList.add(new Ingredient(ingredient.getString("Quantity"),ingredient.getString("IngredientName")));
						}
					try {
						 DatabaseEntry theKey = new DatabaseEntry(recipeName.getBytes("UTF-8"));
						 DatabaseEntry theData = new DatabaseEntry();
						 if (connectionBerkeley.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) { 
						    	retData = theData.getData();
						    	recipeList.add( new Recipe(Id,recipeName,prepTime,cookTime,portions,steps,ingredientsList, retData));
						    } 
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}}});} catch (Exception e) {
						e.printStackTrace();
					}
		return recipeList.get(0);
	}
	
	/**
	 * Cette fonctionnalit� permet de g�n�rer une recette en se basant sur celles existantes 
	 * dans le syst�me. Voici l'algorithme g�n�rale � utiliser :
	 * 
	 * 1- Allez chercher tous les ingr�dients dans votre base de donn�es
	 * 2- Construisez une liste al�atoire d'ingr�dients selon les ingr�dients obtenus � l'�tape pr�c�dente
	 * 3- Cr�ez une liste al�atoire de quelques �tapes bas�e sur une liste pr�d�finie(ex : "M�langez tous les ingr�dients", "cuire au four 20 minutes", etc)
	 * 4- Faites un temps de cuisson, de pr�paration et de nombre de portions al�atoires
	 * 5- Copiez une image d'une autre recette
	 * 6- Construisez un nom en utilisant cette logique :
	 *    - un pr�fixe al�atoire parmi une liste pr�d�finie (ex: ["Giblotte �", "M�lang� de", "Crastillon de"]
	 *    - un suffixe bas� sur un des ingr�dients de la recette (ex: "farine").
	 *    - R�sultat fictif : Crastillon � farine
	 * 
	 * Laissez l'ID de le recette vide, et ne l'ajoutez pas dans la base de donn�es.
	 * 
	 * @return une recette g�n�r�e
	 */
	public static Recipe generateRandomRecipe() {
		Recipe recipe = null; 
		String rdmRecipeName = null;
		Random rand = new Random();
		final List<byte []> image = new ArrayList<>();
		final List<String> keyList = new ArrayList<>();
		final List<Ingredient> ingredientsList = new ArrayList<Ingredient>();
		final List<Ingredient> chosenIngredients = new ArrayList<Ingredient>();
		final List<String> chosenStepList = new ArrayList<String>();
		
		FindIterable<Document> iterator = collection.find();
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					Document ingredients = (Document) document.get("Ingredients");
					String recipeName = document.getString("Name");
					keyList.add(recipeName);
					
					for ( int i =0; i < ingredients.size();i++) {
						Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
						ingredientsList.add(new Ingredient(ingredient.getString("Quantity"),ingredient.getString("IngredientName")));
					}}});
			try {
				 Collections.shuffle(keyList);
				 DatabaseEntry theKey = new DatabaseEntry(keyList.get(0).getBytes("UTF-8"));
				 DatabaseEntry theData = new DatabaseEntry();
				 if (connectionBerkeley.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) { 
				    	image.add( theData.getData());} 
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();}
			
			Collections.shuffle(ingredientsList);
			int nbrIngredients = 1;
			int indexNameIngredient = 0;
			if(ingredientsList.size()>1) {
				 nbrIngredients = rand.nextInt(ingredientsList.size());
			}
			for(int i =0; i<nbrIngredients;i++ ) {
				if(!chosenIngredients.contains(ingredientsList.get(i))) {
				chosenIngredients.add(ingredientsList.get(i));}}
			
			Collections.shuffle(stepList);
			for(int i =0; i<rand.nextInt(stepList.size());i++ ) {
				chosenStepList.add(stepList.get(i));}
			
			if (nbrIngredients >1) { 
				indexNameIngredient = rand.nextInt(chosenIngredients.size());}
			
			Ingredient chosenIngredient = chosenIngredients.get( (indexNameIngredient)); 
			rdmRecipeName = prefixList.get( (rand.nextInt(prefixList.size()))) + chosenIngredient.getName(); 
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		recipe = new Recipe(null,rdmRecipeName.toLowerCase(),rand.nextInt(360),rand.nextInt(360),rand.nextInt(1,10),chosenStepList,ingredientsList,image.get(0));
		return recipe;
	}
	
	/**
	 * Permet d'obtenir une liste de noms de recette similaires � une autre recette
	 * 
	 * - En se basant sur les ingr�dients d'une recette existante (obtenue par recipeId)
	 * - Trouver les recettes qui ont les ingr�dients les plus similaires. 
	 * - Les ordonner DESC (selon le nombre d'ingr�dients similaires), limiter les r�sultats (limit), 
	 * - puis retourner le nom de ces recettes 
	 * 
	 * @param recipeId id de la recette
	 * @param limit nombre � retourner
	 * @return
	 */
	
	public static List<String> getSimilarRecipes(final long recipeId, int limit) {
		List<String> recipeList = new ArrayList<String>();
		final List<String> ingredientsListParamRecipe = new ArrayList<String>();
		final Map<String, Integer> IngredientCommonCount = new HashMap<String, Integer>();
		
		FindIterable<Document> iterator = collection.find(new Document("ID",recipeId));

		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					Document ingredients = (Document) document.get("Ingredients");
					for ( int i =0; i < ingredients.size();i++) {
						Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
						ingredientsListParamRecipe.add(ingredient.getString("IngredientName"));}
		   }});} 
		catch (Exception e) {
			e.printStackTrace();
		}
		 iterator = collection.find();
		try {
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
				List<String> ingredientsListCurrent = new ArrayList<String>();
				Document ingredients = (Document) document.get("Ingredients");
				for ( int i =0; i < ingredients.size();i++) {
					Document ingredient = (Document) ingredients.get("Ingredient"+" "+(i+1));
					ingredientsListCurrent.add(ingredient.getString("IngredientName"));}
				
				//finding intersection : (lien)
				HashSet<String> set = new HashSet<String>();
				set.addAll(ingredientsListParamRecipe);
				set.retainAll(ingredientsListCurrent);
				String[] intersection = {};
				intersection = set.toArray(intersection);
				if (document.getLong("ID")!= recipeId && intersection.length >=1 ) {
				IngredientCommonCount.put(document.getString("Name"),intersection.length );}
		   }});} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
			//sorting array descending: (lien)
		LinkedHashMap<String,Integer> CommonIngredientListSorted = new LinkedHashMap<String,Integer>();
		IngredientCommonCount.entrySet()
							.stream()
							.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 
							.forEachOrdered(x -> CommonIngredientListSorted.put(x.getKey(), x.getValue()));
		
		
		Object[] keys = CommonIngredientListSorted.keySet().toArray() ;
		if(keys.length> limit) {
			for (int i =0 ; i< limit ; i++) {
				recipeList.add((String) keys[i]);
			}
		}
		else {
			for (int i =0 ; i< keys.length ; i++) {
				recipeList.add((String) keys[i]);
			}	
		}
		
		return recipeList;
	}
}
