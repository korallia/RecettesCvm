package ca.qc.cvm.dba.recettes.event;

import ca.qc.cvm.dba.recettes.entity.Recipe;

/**
 * �v�nement utilis� lorsque l'on veut sauvegarder une nouvelle
 * entr�e 
 */
public class SaveEvent extends CommonEvent {
	private Recipe person;
	
	public SaveEvent(Recipe person) {
		super(CommonEvent.Type.Save);
		
		this.person = person;
	}
	
	public Recipe getPerson() {
		return person;
	}
}
