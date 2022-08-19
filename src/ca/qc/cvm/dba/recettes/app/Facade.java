package ca.qc.cvm.dba.recettes.app;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Observer;

import ca.qc.cvm.dba.recettes.entity.Recipe;
import ca.qc.cvm.dba.recettes.event.CommonEvent;

/**
 * Cette classe est l'intermédiaire entre la logique et la vue
 * Entre les panel et le MngApplication. C'est le point d'entrée de la vue
 * vers la logique
 */
public class Facade {
	private static Facade instance;
	
	private MngApplication app;
	
	private Facade() {
		app = new MngApplication();
	}
	
	public static Facade getInstance() {
		if (instance == null) {
			instance = new Facade();
		}
		
		return instance;
	}
	
	public void processEvent(CommonEvent event) {
		app.addEvent(event);
        new Thread(app).start();
	}
	
	public void addObserverClass( PropertyChangeListener pcl) {
		app.addPropertyChangeListener(pcl);
	}
	
	public Recipe getCurrentRecipe() {
		return app.getCurrentRecipe();
	}
	
	public List<Recipe> getRecipeList(String filter, int limit) {
		return app.getRecipeList(filter, limit);
	}
		
	public long getMaxRecipeTime() {
		return app.getMaxRecipeTime();
	}
	
	public long getPhotoCount() {
		return app.getPhotoCount();
	}
	
	public long getRecipeCount() {
		return app.getRecipeCount();
	}
	
	public Recipe getLastAddedRecipe() {
		return app.getLastAddedRecipe();
	}
	
	public Recipe generateRandomRecipe() {
		return app.generateRandomRecipe();
	}
	
	public List<String> getSimilarRecipes(long recipeId, int limit) {
		return app.getSimilarRecipes(recipeId, limit);
	}
	
	public double getAverageNumberOfIngredients() {
		return app.getAverageNumberOfIngredients();
	}
		
	public void exit() {
		app.exit();
	}
}
