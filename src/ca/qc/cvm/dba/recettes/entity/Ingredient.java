package ca.qc.cvm.dba.recettes.entity;

public class Ingredient {
	private String quantity;
	private String name;
	
	public Ingredient(String quantity, String name) {
		this.quantity = quantity;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
}
