package org.ideademo.asmts.pages;

import java.io.StringReader;
import java.io.IOException;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import org.apache.tapestry5.PersistenceConstants;

import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.Persist;


import org.apache.tapestry5.hibernate.HibernateSessionManager;

import org.apache.tapestry5.ioc.annotations.Inject;


import org.hibernate.Session;

import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;

import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;


import org.ideademo.asmts.entities.Asmt;


import org.apache.log4j.Logger;


public class Index 
{
	 
  private static Logger logger = Logger.getLogger(Index.class);
  private static final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_31); 

  
  /////////////////////////////
  //  Drives QBE Search
  @Persist (PersistenceConstants.FLASH)
  private Asmt example;
  
  
  //////////////////////////////////////////////////////////////
  // Used in rendering within Loop just as in Grid (Table) Row
  @SuppressWarnings("unused")
  @Property 
  private Asmt row;

    
  @Property
  @Persist (PersistenceConstants.FLASH)
  private String searchText;

  @Inject
  private Session session;
  
  @Inject
  private HibernateSessionManager sessionManager;


  @Property 
  @Persist (PersistenceConstants.FLASH)
  int retrieved; 
  @Property 
  @Persist (PersistenceConstants.FLASH)
  int total;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Select Boxes - Enumaration values - the user-visible labels are externalized in Index.properties 
  
  
  // the Assessment Type Select box
  @Property
  @Persist (PersistenceConstants.FLASH)
  private Type type;
  
  public enum Type
  {
	SCIENCE, NEEDS, PROBLEM
  }

  // the Are of Applicability Type Select box
  @Property
  @Persist (PersistenceConstants.FLASH)
  private Area area;
  
  public enum Area
  {
	INTERNATIONAL, NATIONAL, LOCAL
  }
  
  
  // the Focus Area select box
  @Property
  @Persist (PersistenceConstants.FLASH)
  private Focus focusarea;
  
  public enum Focus
  {
	 WATER, COASTAL, ECOSYSTEM
  }

  
  // the regions select box
  @Property
  @Persist (PersistenceConstants.FLASH)
  private Regions regions;
  
  public enum Regions
  {
	 // BAS = Pacific Basin, GLB = global - see the properties file 
	 CNP, WNP, SP, BAS, GLB
  }
  
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Entity List generator - QBE, Text Search or Show All 
  //

  @SuppressWarnings("unchecked")
  public List<Asmt> getList()
  {
	//////////////////////////////////
	// first interpret search criteria
	  
	// text search string 
	logger.info("Search Text = " + searchText);
	
	// Construct example for QBE Search by recording what selections have been in the choice boxes on this page  
	if (type != null)  onValueChangedFromType(type.toString());
	if (area != null)  onValueChangedFromArea(area.toString());
	if (focusarea != null) onValueChangedFromFocusArea(focusarea.toString());
	if (regions != null) onValueChangedFromRegions(regions.toString());
	// at this point all the booleans in example have been set.
	// NOTE/MAY BE TODO: Lucene dependency may be removed by setting the text search criteria into various text fields of the example. 
    // Get all records anyway - for showing total at bottom of presentation layer
    List <Asmt> alst = session.createCriteria(Asmt.class).list();
    total = alst.size();

	
    // then makes lists and sublists as per the search criteria 
    List<Asmt> xlst=null; // xlst = Query by Example search List
    if(example != null)
    {
       Example ex = Example.create(example).excludeFalse().ignoreCase().enableLike(MatchMode.ANYWHERE);
       
       xlst = session.createCriteria(Asmt.class).add(ex).list();
       
       
       if (xlst != null)
       {
    	   logger.info("Asmt Example Search Result List Size  = " + xlst.size() );
    	   Collections.sort(xlst);
       }
       else
       {
         logger.info("Asmt Example Search result did not find any results...");
       }
    }
    
    List<Asmt> tlst=null;
    if (searchText != null && searchText.trim().length() > 0)
    {
      FullTextSession fullTextSession = Search.getFullTextSession(sessionManager.getSession());  
      try
      {
        fullTextSession.createIndexer().startAndWait();
       }
       catch (java.lang.InterruptedException e)
       {
         logger.warn("Lucene Indexing was interrupted by something " + e);
       }
      
       QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Asmt.class ).get();
       
       // fields being covered by text search 
       TermMatchingContext onFields = qb
		        .keyword()
		        .onFields("code","name","description", "keywords","contact", "organization", "url", "worksheet", "partners");
       
       BooleanJunction<BooleanJunction> bool = qb.bool();
       /////// Tokenize the search string for default AND logic ///
       TokenStream stream = analyzer.tokenStream(null, new StringReader(searchText));
       CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
       try
       {
        while (stream.incrementToken()) 
         {
    	   String token = cattr.toString();
    	   logger.info("Adding search token " +  token + " to look in Asmts database");
    	   bool.must(onFields.matching(token).createQuery());
         }
        stream.end(); 
        stream.close(); 
       }
       catch (IOException ioe)
       {
    	   logger.warn("Asmts Text Search: Encountered problem tokenizing search term " + searchText);
    	   logger.warn(ioe);
       }
       
       /////////////  the lucene query built from non-simplistic English words 
       org.apache.lucene.search.Query luceneQuery = bool.createQuery();
       
       tlst = fullTextSession.createFullTextQuery(luceneQuery, Asmt.class).list();
       if (tlst != null) 
       {
    	   logger.info("TEXT Search for " + searchText + " found " + tlst.size() + " Asmts records in database");
    	   Collections.sort(tlst);
       }
       else
       {
          logger.info("TEXT Search for " + searchText + " found nothing in Asmts");
       }
    }
    
    
    // organize what type of list is returned...either total, partial (subset) or intersection of various search results  
    if (example == null && (searchText == null || searchText.trim().length() == 0))
    {
    	// Everything...
    	if (alst != null && alst.size() > 0)
    	{
    		logger.info ("Returing all " + alst.size() + " Asmts records");
        	Collections.sort(alst);
    	}
    	else
    	{
    		logger.warn("No Asmt records found in the database");
    	}
    	retrieved = total;
        return alst; 
    }
    else if (xlst == null && tlst != null)
    {
    	// just text search results
    	logger.info("Returing " + tlst.size() + " Asmts records as a result of PURE text search (no QBE) for " + searchText);
    	retrieved = tlst.size();
    	return tlst;
    }
    else if (xlst != null && tlst == null)
    {
    	// just example query results
    	logger.info("Returning " + xlst.size() + " Asmts records as a result of PURE Query-By-Example (QBE), no text string");
    	retrieved = xlst.size();
    	return xlst;
    }
    else 
    {

        ////////////////////////////////////////////
    	// get the INTERSECTION of the two lists
    	
    	// TRIVIAL: if one of them is empty, return the other
    	// if one of them is empty, return the other
    	if (xlst.size() == 0 && tlst.size() > 0)
    	{
        	logger.info("Returing " + tlst.size() + " Asmts records as a result of ONLY text search, QBE pulled up ZERO records for " + searchText);
        	retrieved = tlst.size();
    		return tlst;
    	}

    	if (tlst.size() == 0 && xlst.size() > 0)
    	{
        	logger.info("Returning " + xlst.size() + " Asmts records as a result of ONLY Query-By-Example (QBE), text search pulled up NOTHING for string " + searchText);
        	retrieved = xlst.size();
	        return xlst;
    	}
    	
    	
    	List <Asmt> ivec = new Vector<Asmt>();
    	// if both are empty, return this Empty vector. 
    	if (xlst.size() == 0 && tlst.size() == 0)
    	{
        	logger.info("Neither QBE nor text search for string " + searchText +  " pulled up ANY Asmts Records.");
        	retrieved = 0;
    		return ivec;
    	}
    	


    	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// now deal with BOTH text and QBE being non-empty lists - implementing intersection by Database Primary Key -  Id
    	Iterator<Asmt> xiterator = xlst.iterator();
    	while (xiterator.hasNext()) 
    	{
    		Asmt x = xiterator.next();
    		Long xid = x.getId();
    		
        	Iterator<Asmt> titerator = tlst.iterator();
    		while(titerator.hasNext())
    		{
        		Asmt t = titerator.next();
        		Long tid = t.getId();
    			
        		if (tid == xid)
        		{
        			ivec.add(t); break;
        		}
        		
    		}
    			
    	}
    	// sort again - 
    	if (ivec.size() > 0)  Collections.sort(ivec);
    	logger.info("Returning " + ivec.size() + " Asmts records from COMBINED (text, QBE) Search");
    	retrieved = ivec.size();
    	return ivec;
    }
    
  }
  

  
  ///////////////////////////////////////////////////////////////
  //  Action Event Handlers 
  //
  
  Object onSelectedFromSearch() 
  {
    return null; 
  }

  Object onSelectedFromClear() 
  {
    this.searchText = "";
   
    // nullify selectors 
    type=null;
    area=null;
    focusarea=null;
    regions=null;
    
    this.example = null;
    return null; 
  }
  
  // regions select box listener...may be hooked-up to some AJAX zone if needed (later)
  Object onValueChangedFromRegions(String choice)
  {	
	  // if there is no example
	  
	  if (this.example == null) 
	  {
		  logger.info("Region Select:  Example is NULL");
		  this.example = new Asmt(); 
	  }
	  else
	  {
		  logger.info("Region Select:  Example is NOT null");
	  }
	  logger.info("Region Choice = " + choice);
	  
	  clearRegions(example);
      if (choice == null)
	  {
    	// clear 
	  }
      else if (choice.equalsIgnoreCase("CNP"))
      {
    	example.setCentralNorthPacific(true);
    	logger.info("Example setCentralNorthPacific");
      }
      else if (choice.equalsIgnoreCase("WNP"))
      {
    	example.setWesternNorthPacific(true);
      }
      else if (choice.equalsIgnoreCase("SP"))
      {
    	example.setSouthPacific(true);  
      }
      else if (choice.equalsIgnoreCase("BAS"))
      {
    	example.setPacificBasin(true);   
      }
      else if (choice.equalsIgnoreCase("GLB"))
      {
    	example.setGlobal(true);
      }
      else
      {
    	  // do nothing
      }
      
	  // return request.isXHR() ? editZone.getBody() : null;
      // return index;
      return null;
  }
	
  // Focus select box listener
  Object onValueChangedFromFocusArea(String choice)
  {	
	  // if there is no example
	  
	  if (this.example == null) 
	  {
		  logger.info("Focus Area Select: Example is NULL");
		  this.example = new Asmt(); 
	  }
	  else
	  {
		  logger.info("Focus Area Select: Example is NOT null");
	  }
	  logger.info("Focus Area Choice = " + choice);
	  
	  clearFocusArea(example);
      if (choice == null)
	  {
    	// clear 
	  }
      else if (choice.equalsIgnoreCase("WATER"))
      {
    	example.setWater(true);
      }
      else if (choice.equalsIgnoreCase("COASTAL"))
      {
    	example.setCoastal(true);
      }
      else if (choice.equalsIgnoreCase("ECOSYSTEM"))
      {
    	example.setEcosystem(true);  
      }
      else
      {
    	 // do nothing
      }
      
	  // return request.isXHR() ? editZone.getBody() : null;
      // return index;
      return null;
  }
  
  // Assessment Type box listener
  Object onValueChangedFromType(String choice)
  {	
	  // if there is no example
	  
	  if (this.example == null) 
	  {
		  logger.info("Assessment TYPE Select Value Changed, Example is NULL");
		  this.example = new Asmt(); 
	  }
	  else
	  {
		  logger.info("Assessment TYPE  Select Value Changed, Example is NOT null");
	  }
	  logger.info("Assessment TYPE Chosen = " + choice);
	   
	  clearType(example);
      if (choice == null)
	  {
    	// clear 
	  }
      else if (choice.equalsIgnoreCase("SCIENCE")) 
      {
    	example.setClimateScience(true);  
      }
      else if (choice.equalsIgnoreCase("NEEDS"))
      {
    	example.setNeedsAndCapabilities(true);  
      }
      else if (choice.equalsIgnoreCase("PROBLEM"))
      {
    	example.setRiskVulnerability(true);  
      }
      else
      {
    	 // do nothing
      }
      
	  // return request.isXHR() ? editZone.getBody() : null;
      // return index;
      return null;
  }
  
  // Assessment Type box listener
  Object onValueChangedFromArea(String choice)
  {	
	  // if there is no example
	  
	  if (this.example == null) 
	  {
		  logger.info("Assessment TYPE Select Value Changed, Example is NULL");
		  this.example = new Asmt(); 
	  }
	  else
	  {
		  logger.info("Assessment TYPE  Select Value Changed, Example is NOT null");
	  }
	  logger.info("Assessment TYPE Chosen = " + choice);
	   
	  clearArea(example);
      if (choice == null)
	  {
    	// clear 
	  }
      else if (choice.equalsIgnoreCase("INTERNATIONAL")) 
      {
    	example.setInternational(true);  
      }
      else if (choice.equalsIgnoreCase("NATIONAL"))
      {
    	example.setNational(true);  
      }
      else if (choice.equalsIgnoreCase("LOCAL"))
      {
    	example.setRegional(true);  
      }
      else
      {
    	 // do nothing
      }
      
	  // return request.isXHR() ? editZone.getBody() : null;
      // return index;
      return null;
  }
 
  
  ////////////////////////////////////////////////
  //  QBE Setter 
  //  

  public void setExample(Asmt x) 
  {
    this.example = x;
  }

  
  
  ///////////////////////////////////////////////////////
  // private methods 
  
  private void clearRegions(Asmt asmt)
  {
   	asmt.setCentralNorthPacific(false);
  	asmt.setWesternNorthPacific(false);
  	asmt.setSouthPacific(false);
  	asmt.setPacificBasin(false);
  	asmt.setGlobal(false);
  }
  
  private void clearFocusArea(Asmt asmt)
  {
	asmt.setWater(false);
	asmt.setCoastal(false);
	asmt.setEcosystem(false);
  }
  
  private void clearType(Asmt asmt)
  {
	asmt.setClimateScience(false);
	asmt.setNeedsAndCapabilities(false);
	asmt.setRiskVulnerability(false);
  }
  
  private void clearArea(Asmt asmt)
  {
	asmt.setInternational(false);
	asmt.setNational(false);
	asmt.setRegional(false);
  }

}