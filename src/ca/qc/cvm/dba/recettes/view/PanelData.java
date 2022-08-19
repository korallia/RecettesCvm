package ca.qc.cvm.dba.recettes.view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import ca.qc.cvm.dba.recettes.app.Facade;
import ca.qc.cvm.dba.recettes.entity.Recipe;
import ca.qc.cvm.dba.recettes.view.util.BackgroundPanel;

public class PanelData extends CommonPanel {
	private static final long serialVersionUID = 1L;
	
	private JLabel avgIngredients; 
	private JLabel recipeCount;
	private JLabel photoCount;
	private JLabel lastRecipe;
	private JLabel maxTime;

	public PanelData(int width, int height) throws Exception {
		super(width, height, true, "assets/images/background-data.jpg");
		jbInit();
	}
	
	@Override
	protected void jbInit() throws Exception {		
		int y = 20;
		super.addLabel("Nombre de recettes sauvegard�es", 20, y, 400, 30);
		recipeCount = super.addLabel("", 500, y, 100, 30);
		recipeCount.setHorizontalAlignment(JLabel.CENTER);
		recipeCount.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		
		y += 40;
		super.addLabel("Nombre de photos sauvegard�es", 20, y, 400, 30);
		photoCount = super.addLabel("", 500, y, 100, 30);
		photoCount.setHorizontalAlignment(JLabel.CENTER);
		photoCount.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		
		y += 40;
		super.addLabel("Nombre moyen d'ingr�dients", 20, y, 400, 30);
		avgIngredients = super.addLabel("", 500, y,100, 30);
		avgIngredients.setHorizontalAlignment(JLabel.CENTER);
		avgIngredients.setBorder(BorderFactory.createLineBorder(Color.WHITE));

		y += 40;
		super.addLabel("Temps de la recette la plus longue (pr�p. + cuisson)", 20, y, 400, 30);
		maxTime= super.addLabel("", 500, y, 100, 30);
		maxTime.setHorizontalAlignment(JLabel.CENTER);
		maxTime.setBorder(BorderFactory.createLineBorder(Color.WHITE));

		y += 40;
		super.addLabel("Nom de la recette la plus r�cemment ajout�e", 20, y, 400, 30);
		lastRecipe = super.addLabel("", 500, y, 250, 30);
		lastRecipe.setHorizontalAlignment(JLabel.CENTER);
		lastRecipe.setBorder(BorderFactory.createLineBorder(Color.WHITE));
	}
	
	/**
	 * Cette m�thode est appel�e automatiquement � chaque fois qu'un panel est affich� (lorsqu'on arrive sur la page)
	 */
	@Override
	public void resetUI() {
		avgIngredients.setText(Facade.getInstance().getAverageNumberOfIngredients() + "");
		photoCount.setText(Facade.getInstance().getPhotoCount() + "");
		recipeCount.setText(Facade.getInstance().getRecipeCount() + "");
		Recipe r = Facade.getInstance().getLastAddedRecipe();
		lastRecipe.setText(r == null ? "-" : r.getName());
		maxTime.setText(Facade.getInstance().getMaxRecipeTime() + "");
		
	}

}
