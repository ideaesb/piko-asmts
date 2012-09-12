package org.ideademo.asmts.entities;


import java.lang.Comparable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import org.ideademo.asmts.entities.Asmt;

import org.apache.tapestry5.beaneditor.NonVisual;

/**
 * Assessments Worksheet
 * @author Uday
 *
 */
@Entity @Indexed
public class Asmt implements Comparable<Asmt>
{
	
	//////////////////////////////////////////
	//Reserved indexing id 
	
	@Id @GeneratedValue @DocumentId @NonVisual
	private Long id;
	
	
	//////////////////////////////////////////////
	//String fields (being a keyword for Lucene)
	//
	
	@Field
	private String code="";

	@Field @Column (length=1024)
	private String name="";
	
	@Field @Column (length=2048)
	private String organization="";
	
	@Field @Column (length=2048)
	private String partners="";

	@Field @Column (length=2048)
	private String contact="";
	
	@Field 
	private String url="";
	
	@Field @Column (length=4096)
	private String description="";
	
	@Field @Column (length=4096)
	private String keywords="";
	
	@Field 
	private String worksheet="";
	
	
	
  /////////////////////////////////////////////////////////////////////////////
  //  Embedded (Group of Booleans) 
  //
  
  // Asssessment Type
  private boolean climateScience = false;
  private boolean needsAndCapabilities = false;
	private boolean needs = false;
	private boolean capacity = false;
	private boolean capabilities = false;
  private boolean riskVulnerability = false;

  
  // Area of Applicability
  private boolean international= false;
  private boolean national= false;
  private boolean regional= false;
  

  // Focus Area
  private boolean water = false;
  private boolean coastal = false; 
  private boolean ecosystem = false;
  
  
  //Region/Locale
  private boolean centralNorthPacific = false;
    private boolean stateOfHawaii = false;
      private boolean northWestHawaiianIslands = false;
      private boolean pacificRemoteIslands = false;

  private boolean westernNorthPacific = false;
    private boolean cnmi = false;
    private boolean fsm = false;
    private boolean guam = false;
    private boolean palau = false;
    private boolean rmi = false;
    private boolean otherWesternNorthPacific = false;
    
  private boolean southPacific = false;
    private boolean americanSamoa = false;
    private boolean australia = false;
    private boolean cookIslands = false; 
    private boolean fiji = false;
    private boolean frenchPolynesia = false;
    private boolean kiribati = false; 
    private boolean newZealand = false;
    private boolean png = false; 
    private boolean samoa = false;
    private boolean solomonIslands = false; 
    private boolean tonga = false;
    private boolean tuvalu = false; 
    private boolean vanuatu = false; 
    private boolean otherSouthPacific = false;
    
  private boolean pacificBasin = false;
  private boolean global = false;
  
  // Status
  private boolean completed = false;
  private boolean ongoing = false;
  private boolean planned = false;
  private boolean proposed = false;

  
  /////////////////////////////////////////////
  //  internal flags
  
  private boolean worksheetExists = false;

  
  //////////////////////////////////////
  // getters, setters
  // 
  
  public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOrganization() {
		return organization;
	}
	public String getPartners() {
		return partners;
	}
	public void setPartners(String partners) {
		this.partners = partners;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getKeywords() {
		return keywords;
	}
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	public String getWorksheet() {
		return worksheet;
	}
	public void setWorksheet(String worksheet) {
		this.worksheet = worksheet;
	}
	public boolean isClimateScience() {
		return climateScience;
	}
	public void setClimateScience(boolean climateScience) {
		this.climateScience = climateScience;
	}
	public boolean isNeedsAndCapabilities() {
		return needsAndCapabilities;
	}
	public void setNeedsAndCapabilities(boolean needsAndCapabilities) {
		this.needsAndCapabilities = needsAndCapabilities;
	}
	public boolean isNeeds() {
		return needs;
	}
	public void setNeeds(boolean needs) {
		this.needs = needs;
	}
	public boolean isCapacity() {
		return capacity;
	}
	public void setCapacity(boolean capacity) {
		this.capacity = capacity;
	}
	public boolean isCapabilities() {
		return capabilities;
	}
	public void setCapabilities(boolean capabilities) {
		this.capabilities = capabilities;
	}
	public boolean isRiskVulnerability() {
		return riskVulnerability;
	}
	public void setRiskVulnerability(boolean riskVulnerability) {
		this.riskVulnerability = riskVulnerability;
	}
	public boolean isInternational() {
		return international;
	}
	public void setInternational(boolean international) {
		this.international = international;
	}
	public boolean isNational() {
		return national;
	}
	public void setNational(boolean national) {
		this.national = national;
	}
	public boolean isRegional() {
		return regional;
	}
	public void setRegional(boolean regional) {
		this.regional = regional;
	}
	public boolean isWater() {
		return water;
	}
	public void setWater(boolean water) {
		this.water = water;
	}
	public boolean isCoastal() {
		return coastal;
	}
	public void setCoastal(boolean coastal) {
		this.coastal = coastal;
	}
	public boolean isEcosystem() {
		return ecosystem;
	}
	public void setEcosystem(boolean ecosystem) {
		this.ecosystem = ecosystem;
	}
	public boolean isCentralNorthPacific() {
		return centralNorthPacific;
	}
	public void setCentralNorthPacific(boolean centralNorthPacific) {
		this.centralNorthPacific = centralNorthPacific;
	}
	public boolean isStateOfHawaii() {
		return stateOfHawaii;
	}
	public void setStateOfHawaii(boolean stateOfHawaii) {
		this.stateOfHawaii = stateOfHawaii;
	}
	public boolean isNorthWestHawaiianIslands() {
		return northWestHawaiianIslands;
	}
	public void setNorthWestHawaiianIslands(boolean northWestHawaiianIslands) {
		this.northWestHawaiianIslands = northWestHawaiianIslands;
	}
	public boolean isPacificRemoteIslands() {
		return pacificRemoteIslands;
	}
	public void setPacificRemoteIslands(boolean pacificRemoteIslands) {
		this.pacificRemoteIslands = pacificRemoteIslands;
	}
	public boolean isWesternNorthPacific() {
		return westernNorthPacific;
	}
	public void setWesternNorthPacific(boolean westernNorthPacific) {
		this.westernNorthPacific = westernNorthPacific;
	}
	public boolean isCnmi() {
		return cnmi;
	}
	public void setCnmi(boolean cnmi) {
		this.cnmi = cnmi;
	}
	public boolean isFsm() {
		return fsm;
	}
	public void setFsm(boolean fsm) {
		this.fsm = fsm;
	}
	public boolean isGuam() {
		return guam;
	}
	public void setGuam(boolean guam) {
		this.guam = guam;
	}
	public boolean isPalau() {
		return palau;
	}
	public void setPalau(boolean palau) {
		this.palau = palau;
	}
	public boolean isRmi() {
		return rmi;
	}
	public void setRmi(boolean rmi) {
		this.rmi = rmi;
	}
	public boolean isOtherWesternNorthPacific() {
		return otherWesternNorthPacific;
	}
	public void setOtherWesternNorthPacific(boolean otherWesternNorthPacific) {
		this.otherWesternNorthPacific = otherWesternNorthPacific;
	}
	public boolean isSouthPacific() {
		return southPacific;
	}
	public void setSouthPacific(boolean southPacific) {
		this.southPacific = southPacific;
	}
	public boolean isAmericanSamoa() {
		return americanSamoa;
	}
	public void setAmericanSamoa(boolean americanSamoa) {
		this.americanSamoa = americanSamoa;
	}
	public boolean isAustralia() {
		return australia;
	}
	public void setAustralia(boolean australia) {
		this.australia = australia;
	}
	public boolean isCookIslands() {
		return cookIslands;
	}
	public void setCookIslands(boolean cookIslands) {
		this.cookIslands = cookIslands;
	}
	public boolean isFiji() {
		return fiji;
	}
	public void setFiji(boolean fiji) {
		this.fiji = fiji;
	}
	public boolean isFrenchPolynesia() {
		return frenchPolynesia;
	}
	public void setFrenchPolynesia(boolean frenchPolynesia) {
		this.frenchPolynesia = frenchPolynesia;
	}
	public boolean isKiribati() {
		return kiribati;
	}
	public void setKiribati(boolean kiribati) {
		this.kiribati = kiribati;
	}
	public boolean isNewZealand() {
		return newZealand;
	}
	public void setNewZealand(boolean newZealand) {
		this.newZealand = newZealand;
	}
	public boolean isPng() {
		return png;
	}
	public void setPng(boolean png) {
		this.png = png;
	}
	public boolean isSamoa() {
		return samoa;
	}
	public void setSamoa(boolean samoa) {
		this.samoa = samoa;
	}
	public boolean isSolomonIslands() {
		return solomonIslands;
	}
	public void setSolomonIslands(boolean solomonIslands) {
		this.solomonIslands = solomonIslands;
	}
	public boolean isTonga() {
		return tonga;
	}
	public void setTonga(boolean tonga) {
		this.tonga = tonga;
	}
	public boolean isTuvalu() {
		return tuvalu;
	}
	public void setTuvalu(boolean tuvalu) {
		this.tuvalu = tuvalu;
	}
	public boolean isVanuatu() {
		return vanuatu;
	}
	public void setVanuatu(boolean vanuatu) {
		this.vanuatu = vanuatu;
	}
	public boolean isOtherSouthPacific() {
		return otherSouthPacific;
	}
	public void setOtherSouthPacific(boolean otherSouthPacific) {
		this.otherSouthPacific = otherSouthPacific;
	}
	public boolean isPacificBasin() {
		return pacificBasin;
	}
	public void setPacificBasin(boolean pacificBasin) {
		this.pacificBasin = pacificBasin;
	}
	public boolean isGlobal() {
		return global;
	}
	public void setGlobal(boolean global) {
		this.global = global;
	}
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	public boolean isOngoing() {
		return ongoing;
	}
	public void setOngoing(boolean ongoing) {
		this.ongoing = ongoing;
	}
	public boolean isPlanned() {
		return planned;
	}
	public void setPlanned(boolean planned) {
		this.planned = planned;
	}
	public boolean isProposed() {
		return proposed;
	}
	public void setProposed(boolean proposed) {
		this.proposed = proposed;
	}


	public boolean isWorksheetExists() {
		return worksheetExists;
	}
	public void setWorksheetExists(boolean worksheetExists) {
		this.worksheetExists = worksheetExists;
	}
	
	
	////////////////////////////////////////////////
	/// default/natural sort order - String  - names
	
	public int compareTo(Asmt ao) 
	{
	    boolean thisIsEmpty = false;
	    boolean aoIsEmpty = false; 
	    
	    if (this.getName() == null || this.getName().trim().length() == 0) thisIsEmpty = true; 
	    if (ao.getName() == null || ao.getName().trim().length() == 0) aoIsEmpty = true;
	    
	    if (thisIsEmpty && aoIsEmpty) return 0;
	    if (thisIsEmpty && !aoIsEmpty) return -1;
	    if (!thisIsEmpty && aoIsEmpty) return 1; 
	    return this.getName().compareToIgnoreCase(ao.getName());
    }
}
