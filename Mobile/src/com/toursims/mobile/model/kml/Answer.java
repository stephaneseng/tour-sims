package com.toursims.mobile.model.kml;

import org.simpleframework.xml.Element;

public class Answer {
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	
	@Element(required=false)
	private String isTrue;

	@Element(required=false)
	private String value;
	
	public String getIsTrue() {
		return isTrue;
	}
	public String getValue() {
		return value;
	}
	
	public void setIsTrue(String isTrue) {
		this.isTrue = isTrue;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public boolean isTrue() {
		return isTrue.equals(TRUE);
	}
	
}
