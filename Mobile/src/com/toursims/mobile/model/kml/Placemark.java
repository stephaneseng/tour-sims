package com.toursims.mobile.model.kml;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root
public class Placemark {
	public static final String NAME = "Placemark_NAME";

	private Integer id;

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@Element(required = false)
	private String clueTitle;

	@Element(required = false)
	private String direction;

	@Element(required = false)
	private String name;

	@Element(required = false)
	private String description;

	@Element(required = false)
	private String hint;

	@Element(required = false)
	private String greetings;

	@Element(required = false)
	private String address;

	@ElementList(required = false)
	private List<Style> StyleMap;

	@Element(required = false)
	private Point Point;

	@Element(required = false)
	private LookAt LookAt;

	@Element(required = false)
	private GeometryCollection GeometryCollection;

	@Element(required = false)
	private String styleUrl;

	@ElementList(entry = "Data", required = false)
	private List<Data> ExtendedData;

	@ElementList(entry = "Question", required = false)
	private List<Question> Questions;

	@Element(required = false)
	private String routePlacemark = "false";

	@Element(required = false)
	private TimeSpan TimeSpan;

	public List<Data> getExtendedData() {
		return ExtendedData;
	}

	public List<Question> getQuestions() {
		return Questions;
	}

	public void setQuestions(List<Question> questions) {
		Questions = questions;
	}

	public void setExtendedData(List<Data> extendedData) {
		this.ExtendedData = extendedData;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public List<Style> getStyleMap() {
		return StyleMap;
	}

	public void setStyleMap(List<Style> styleMap) {
		this.StyleMap = styleMap;
	}

	public Point getPoint() {
		return Point;
	}

	public void setPoint(Point point) {
		this.Point = point;
	}

	public void setPoint(String coordinates) {
		this.Point = new Point(coordinates);
	}

	public LookAt getLookAt() {
		return LookAt;
	}

	public void setLookAt(LookAt lookAt) {
		this.LookAt = lookAt;
	}

	public GeometryCollection getGeometryCollection() {
		return GeometryCollection;
	}

	public void setGeometryCollection(GeometryCollection geometryCollection) {
		GeometryCollection = geometryCollection;
	}

	public String getStyleUrl() {
		return styleUrl;
	}

	public void setStyleUrl(String styleUrl) {
		this.styleUrl = styleUrl;
	}

	public void setGreetings(String greetings) {
		this.greetings = greetings;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getGreetings() {
		return greetings;
	}

	public String getHint() {
		return hint;
	}

	public String getDirection() {
		if (direction == null) {
			return description;
		}
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getClueTitle() {
		if (clueTitle == null) {
			return name;
		}
		return clueTitle;
	}

	public void setClueTitle(String clueTitle) {
		this.clueTitle = clueTitle;
	}

	public boolean isRoutePlacemark() {
		return routePlacemark.equals("true");
	}

	public String getRoutePlacemark() {
		return routePlacemark;
	}

	public void setRoutePlacemark(String routePlacemark) {
		this.routePlacemark = routePlacemark;
	}

	public String toKml() {
		String s = "<Placemark>";
		s += Point.toKml();
		s += TimeSpan.toKml();
		s += "</Placemark>";
		return s;
	}

	public Placemark(double longitude, double latitude, String begin) {
		super();
		Point p = new Point();
		p.setCoordinates(Double.toString(longitude) + ","
				+ Double.toString(latitude) + ",0");
		TimeSpan = new TimeSpan();
		TimeSpan.setBegin(begin);
		this.Point = p;
	}

	public Placemark() {
		super();
	}

	public TimeSpan getTimeSpan() {
		return TimeSpan;
	}

	public void setTimeSpan(TimeSpan timeSpan) {
		TimeSpan = timeSpan;
	}

}