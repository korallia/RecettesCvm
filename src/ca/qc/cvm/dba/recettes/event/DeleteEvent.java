package ca.qc.cvm.dba.recettes.event;

import ca.qc.cvm.dba.recettes.entity.Recipe;

/**
 * �v�nement utilis� lorsque l'on veut supprimer une entr�e
 */
public class DeleteEvent extends CommonEvent {
	private Recipe person;
	
	public DeleteEvent(Recipe person) {
		super(CommonEvent.Type.Delete);
		
		this.person = person;
	}
	
	public Recipe getPerson() {
		return person;
	}
}
