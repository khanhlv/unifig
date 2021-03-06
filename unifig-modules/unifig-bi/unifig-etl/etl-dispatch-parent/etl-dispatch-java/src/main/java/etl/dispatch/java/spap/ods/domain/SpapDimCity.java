package etl.dispatch.java.spap.ods.domain;

import java.io.Serializable;

public class SpapDimCity implements Serializable {
	private static final long serialVersionUID = 1640734798246539965L;

	private int id;

	private int countryId;

	private int regionId;

	private String name;
	
	public SpapDimCity(){
		
	}
	public SpapDimCity(int id, int countryId, int regionId, String name){
		this.id =id;
		this.countryId = countryId;
		this.regionId =regionId;
		this.name =name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCountryId() {
		return countryId;
	}

	public void setCountryId(int countryId) {
		this.countryId = countryId;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		try {
			SpapDimCity cloned = (SpapDimCity) super.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			return new SpapDimCity(this.id, this.countryId, this.regionId, this.name);
		}
	}

}
