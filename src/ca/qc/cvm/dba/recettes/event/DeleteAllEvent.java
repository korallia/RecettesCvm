package ca.qc.cvm.dba.recettes.event;

import ca.qc.cvm.dba.recettes.entity.Recipe;

/**
 * �v�nement utilis� lorsque l'on veut supprimer toute les donn�es
 */
public class DeleteAllEvent extends CommonEvent {
	
	public DeleteAllEvent() {
		super(CommonEvent.Type.DeleteAll);
	}
}
