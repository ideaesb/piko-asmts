package org.ideademo.asmts.pages;

import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.ideademo.asmts.entities.Asmt;
import org.ideademo.asmts.services.util.PDFStreamResponse;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.log4j.Logger;


public class Index 
{
	 
  private static Logger logger = Logger.getLogger(Index.class);
  private static final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_31); 

  
  /////////////////////////////
  //  Drives QBE Search
  @Persist
  private Asmt example;
  
  
  //////////////////////////////////////////////////////////////
  // Used in rendering within Loop just as in Grid (Table) Row
  @SuppressWarnings("unused")
  @Property 
  private Asmt row;

    
  @Property
  @Persist
  private String searchText;

  @Inject
  private Session session;
  
  @Inject
  private HibernateSessionManager sessionManager;


  @Property 
  @Persist
  int retrieved; 
  @Property 
  @Persist
  int total;
  
  @Inject
  @Path("context:layout/images/image067.gif")
  private Asset logoAsset;
  
  @Inject
  private AssetSource assetSource;
  
  @Inject
  Messages messages;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Select Boxes - Enumaration values - the user-visible labels are externalized in Index.properties 
  
  
  // the Assessment Type Select box
  @Property
  @Persist
  private Type type;
  
  public enum Type
  {
	SCIENCE, NEEDS, PROBLEM
  }

  // the Are of Applicability Type Select box
  @Property
  @Persist
  private Area area;
  
  public enum Area
  {
	INTERNATIONAL, NATIONAL, LOCAL
  }
  
  
  // the Focus Area select box
  @Property
  @Persist
  private Focus focusarea;
  
  public enum Focus
  {
	 WATER, COASTAL, ECOSYSTEM
  }

  
  // the regions select box
  @Property
  @Persist
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

  public StreamResponse onSelectedFromPdf() 
  {
      // Create PDF
      InputStream is = getPdfTable(getList());
      // Return response
      return new PDFStreamResponse(is,"PacisAssessments" + System.currentTimeMillis());
  }

  private InputStream getPdfTable(List list) 
  {

      // step 1: creation of a document-object
      Document document = new Document();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
              // step 2:
              // we create a writer that listens to the document
              // and directs a PDF-stream to a file
              PdfWriter writer = PdfWriter.getInstance(document, baos);
              // step 3: we open the document
              document.open();
              
              java.awt.Image awtImage = Toolkit.getDefaultToolkit().createImage(logoAsset.getResource().toURL());
              if (awtImage != null)
              {
            	  com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(awtImage, null); 
            	  logo.scalePercent(50);
            	  if (logo != null) document.add(logo);
              }

              DateFormat formatter = new SimpleDateFormat
                      ("EEE MMM dd HH:mm:ss zzz yyyy");
                  Date date = new Date(System.currentTimeMillis());
                  TimeZone eastern = TimeZone.getTimeZone("Pacific/Honolulu");
                  formatter.setTimeZone(eastern);

              document.add(new Paragraph("Piko Assessments " + formatter.format(date)));
              
              String subheader = "Printing " + retrieved + " of total " + total + " records.";
              if (StringUtils.isNotBlank(searchText))
              {
            	  subheader += "  Searching for \"" + searchText + "\""; 
              }
              
              document.add(new Paragraph(subheader));
              document.add(Chunk.NEWLINE);document.add(Chunk.NEWLINE);
              
              // create table, 2 columns
           	Iterator<Asmt> iterator = list.iterator();
           	int count=0;
       		while(iterator.hasNext())
      		{
       			count++;
          		Asmt asmt = iterator.next();
          		
          		String acronym = StringUtils.trimToEmpty(asmt.getCode());
          		String name = StringUtils.trimToEmpty(asmt.getName());
          		String description = StringUtils.trimToEmpty(asmt.getDescription());
          		String leadAgencies = StringUtils.trimToEmpty(asmt.getOrganization());
          		String contacts = StringUtils.trimToEmpty(asmt.getContact());
          		String partnering = StringUtils.trimToEmpty(asmt.getPartners());
          		String url = StringUtils.trimToEmpty(asmt.getUrl());
          		
          		
                PdfPTable table = new PdfPTable(2);
                table.setWidths(new int[]{1, 4});
                table.setSplitRows(false);
                
                PdfPCell nameTitle = new PdfPCell(new Phrase("#" + count + ") Name")); 
                
                if (StringUtils.isNotBlank(acronym)) name = name + " (" + acronym + ")";
                PdfPCell nameCell = new PdfPCell(new Phrase(name));
                
                nameTitle.setBackgroundColor(BaseColor.CYAN);  nameCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                
                table.addCell(nameTitle);  table.addCell(nameCell);          		          		
          		
          		if (StringUtils.isNotBlank(leadAgencies))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Lead Agencies")));  table.addCell(new PdfPCell(new Phrase(leadAgencies)));
          		}

          		if (StringUtils.isNotBlank(contacts))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Contacts")));  table.addCell(new PdfPCell(new Phrase(contacts)));
          		}
                
          		if (StringUtils.isNotBlank(partnering))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Partnering Agencies")));  table.addCell(new PdfPCell(new Phrase(partnering)));
          		}


          	    // compile the types list
          		com.itextpdf.text.List types = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if (asmt.isClimateScience()) 
          		{
          			ListItem item = new ListItem(getLabel("climateScience")); types.add(item);
          		}
          		if (asmt.isNeedsAndCapabilities()) 
          		{
          			ListItem item = new ListItem(getLabel("needsAndCapabilities")); types.add(item);
          		}
          		if (asmt.isNeeds()) 
          		{
          			ListItem item = new ListItem(getLabel("needs")); types.add(item);
          		}
          		if (asmt.isCapacity()) 
          		{
          			ListItem item = new ListItem(getLabel("capacity")); types.add(item);
          		}
          		if (asmt.isCapabilities()) 
          		{
          			ListItem item = new ListItem(getLabel("capabilities")); types.add(item);
          		}
          		if (asmt.isRiskVulnerability()) 
          		{
          			ListItem item = new ListItem(getLabel("riskVulnerability")); types.add(item);
          		}


          		if(types.size() > 0)
          		{
          		  PdfPCell typesCell = new PdfPCell(); typesCell.addElement(types);
          		  table.addCell(new PdfPCell(new Phrase("Types")));  table.addCell(typesCell);
          		}
          		
          		
                //Aoa
          		com.itextpdf.text.List aoa = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if(asmt.isInternational())
          		{
          			ListItem item = new ListItem(getLabel("international")); aoa.add(item);
          		}
          		if(asmt.isNational())
          		{
          			ListItem item = new ListItem(getLabel("national")); aoa.add(item);
          		}
          		if(asmt.isRegional())
          		{
          			ListItem item = new ListItem(getLabel("regional")); aoa.add(item);
          		}
          		
          		if (aoa.size() > 0)
          		{
           		  PdfPCell aoaCell = new PdfPCell(); aoaCell.addElement(aoa);
           		  table.addCell(new PdfPCell(new Phrase("Area of Applicability")));  table.addCell(aoaCell);
          		}
          		
          		// focus area
          		com.itextpdf.text.List fa = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if(asmt.isWater())
          		{
          			ListItem item = new ListItem(getLabel("water")); fa.add(item);
          		}
          		if(asmt.isCoastal())
          		{
          			ListItem item = new ListItem(getLabel("coastal")); fa.add(item);
          		}
          		if(asmt.isEcosystem())
          		{
          			ListItem item = new ListItem(getLabel("ecosystem")); fa.add(item);
          		}
          		
          		if (fa.size() > 0)
          		{
           		  PdfPCell faCell = new PdfPCell(); faCell.addElement(fa);
           		  table.addCell(new PdfPCell(new Phrase("Focus Area")));  table.addCell(faCell);
          		}

               
          		//region
          		com.itextpdf.text.List regions = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if (asmt.isCentralNorthPacific())
          		{
          			ListItem item = new ListItem(getLabel("centralNorthPacific")); regions.add(item);
          		}
          		if (asmt.isStateOfHawaii())
          		{
          			ListItem item = new ListItem(getLabel("stateOfHawaii")); regions.add(item);
          		}
          		if (asmt.isNorthWestHawaiianIslands())
          		{
          			ListItem item = new ListItem(getLabel("northWesternHawaiianIslands")); regions.add(item);
          		}
          		if (asmt.isPacificRemoteIslands())
          		{
          			ListItem item = new ListItem(getLabel("pacificRemoteIslands")); regions.add(item);
          		}
          		if (asmt.isWesternNorthPacific())
          		{
          			ListItem item = new ListItem(getLabel("westernNorthPacific")); regions.add(item);
          		}
          		if (asmt.isCnmi())
          		{
          			ListItem item = new ListItem(getLabel("cnmi")); regions.add(item);
          		}
          		if (asmt.isFsm())
          		{
          			ListItem item = new ListItem(getLabel("fsm")); regions.add(item);
          		}
          		if (asmt.isGuam())
          		{
          			ListItem item = new ListItem(getLabel("guam")); regions.add(item);
          		}
          		if (asmt.isPalau())
          		{
          			ListItem item = new ListItem(getLabel("palau")); regions.add(item);
          		}
          		if (asmt.isRmi())
          		{
          			ListItem item = new ListItem(getLabel("rmi")); regions.add(item);
          		}
          		if (asmt.isOtherWesternNorthPacific())
          		{
          			ListItem item = new ListItem(getLabel("otherWesternNorthPacific")); regions.add(item);
          		}
          		if (asmt.isSouthPacific())
          		{
          			ListItem item = new ListItem(getLabel("southPacific")); regions.add(item);
          		}
          		if (asmt.isAmericanSamoa())
          		{
          			ListItem item = new ListItem(getLabel("americanSamoa")); regions.add(item);
          		}
          		if (asmt.isAustralia())
          		{
          			ListItem item = new ListItem(getLabel("australia")); regions.add(item);
          		}
          		if (asmt.isCookIslands())
          		{
          			ListItem item = new ListItem(getLabel("cookIslands")); regions.add(item);
          		}
          		if (asmt.isFiji())
          		{
          			ListItem item = new ListItem(getLabel("fiji")); regions.add(item);
          		}
          		if (asmt.isFrenchPolynesia())
          		{
          			ListItem item = new ListItem(getLabel("frenchPolynesia")); regions.add(item);
          		}
          		if (asmt.isKiribati())
          		{
          			ListItem item = new ListItem(getLabel("kiribati")); regions.add(item);
          		}
          		if (asmt.isNewZealand())
          		{
          			ListItem item = new ListItem(getLabel("newZealand")); regions.add(item);
          		}
          		if (asmt.isPng())
          		{
          			ListItem item = new ListItem(getLabel("png")); regions.add(item);
          		}
          		if (asmt.isSamoa())
          		{
          			ListItem item = new ListItem(getLabel("samoa")); regions.add(item);
          		}
          		if (asmt.isSolomonIslands())
          		{
          			ListItem item = new ListItem(getLabel("solomonIslands")); regions.add(item);
          		}
          		if (asmt.isTonga())
          		{
          			ListItem item = new ListItem(getLabel("tonga")); regions.add(item);
          		}
          		if (asmt.isTuvalu())
          		{
          			ListItem item = new ListItem(getLabel("tuvalu")); regions.add(item);
          		}
          		if (asmt.isVanuatu())
          		{
          			ListItem item = new ListItem(getLabel("vanuatu")); regions.add(item);
          		}
          		if (asmt.isOtherSouthPacific())
          		{
          			ListItem item = new ListItem(getLabel("otherSouthPacific")); regions.add(item);
          		}
          		if (asmt.isPacificBasin())
          		{
          			ListItem item = new ListItem(getLabel("pacificBasin")); regions.add(item);
          		}
          		if (asmt.isGlobal())
          		{
          			ListItem item = new ListItem(getLabel("global")); regions.add(item);
          		}
          		
        		
          		if (regions.size() > 0)
          		{
           		  PdfPCell rCell = new PdfPCell(); rCell.addElement(regions);
           		  table.addCell(new PdfPCell(new Phrase("Regions")));  table.addCell(rCell);
          		}
          		
          		
          		//status
          		com.itextpdf.text.List status = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if (asmt.isCompleted())
          		{
          			ListItem item = new ListItem(getLabel("completed")); status.add(item);
          		}
          		if (asmt.isOngoing())
          		{
          			ListItem item = new ListItem(getLabel("ongoing")); status.add(item);
          		}
          		if (asmt.isPlanned())
          		{
          			ListItem item = new ListItem(getLabel("planned")); status.add(item);
          		}
          		if (asmt.isProposed())
          		{
          			ListItem item = new ListItem(getLabel("proposed")); status.add(item);
          		}
          		
          		if (status.size() > 0)
          		{
           		  PdfPCell sCell = new PdfPCell(); sCell.addElement(status);
           		  table.addCell(new PdfPCell(new Phrase("Status")));  table.addCell(sCell);
          		}
          		
          		
          		
          		
          		if (StringUtils.isNotBlank(description))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Description")));  table.addCell(new PdfPCell(new Phrase(description)));
          		}

          		if (StringUtils.isNotBlank(url))
          		{
            	  Anchor link = new Anchor(StringUtils.trimToEmpty(url)); link.setReference(StringUtils.trimToEmpty(url));
          		  table.addCell(new PdfPCell(new Phrase("Url")));  table.addCell(new PdfPCell(link));
          		}

          		
          		document.add(table);
          		document.add(Chunk.NEWLINE);
      		}
              
              
      } catch (DocumentException de) {
              logger.fatal(de.getMessage());
      }
      catch (IOException ie)
      {
    	 logger.warn("Could not find NOAA logo (likely)");
    	 logger.warn(ie);
      }

      // step 5: we close the document
      document.close();
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      return bais;
}

  public StreamResponse onReturnStreamResponse(long id) 
  {
	  Asmt asmt =  (Asmt) session.load(Asmt.class, id);
      // step 1: creation of a document-object
      Document document = new Document();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
              // step 2:
              // we create a writer that listens to the document
              // and directs a PDF-stream to a file
              PdfWriter writer = PdfWriter.getInstance(document, baos);
              // step 3: we open the document
              document.open();
              
              java.awt.Image awtImage = Toolkit.getDefaultToolkit().createImage(logoAsset.getResource().toURL());
              if (awtImage != null)
              {
            	  com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(awtImage, null); 
            	  logo.scalePercent(50);
            	  if (logo != null) document.add(logo);
              }

              DateFormat formatter = new SimpleDateFormat
                      ("EEE MMM dd HH:mm:ss zzz yyyy");
                  Date date = new Date(System.currentTimeMillis());
                  TimeZone eastern = TimeZone.getTimeZone("Pacific/Honolulu");
                  formatter.setTimeZone(eastern);

              document.add(new Paragraph("Piko Assessments " + formatter.format(date)));
              
              
              document.add(Chunk.NEWLINE);document.add(Chunk.NEWLINE);
              
          		document.add(getPDFTable(asmt));
          		document.add(Chunk.NEWLINE);
      		
              
              
      } catch (DocumentException de) {
              logger.fatal(de.getMessage());
      }
      catch (IOException ie)
      {
    	 logger.warn("Could not find NOAA logo (likely)");
    	 logger.warn(ie);
      }

      // step 5: we close the document
      document.close();
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      return new PDFStreamResponse(bais,"PacisAssessment" + System.currentTimeMillis());
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

  private PdfPTable getPDFTable(Asmt asmt)
  {
      // create table, 2 columns
  		String acronym = StringUtils.trimToEmpty(asmt.getCode());
  		String name = StringUtils.trimToEmpty(asmt.getName());
  		String description = StringUtils.trimToEmpty(asmt.getDescription());
  		String leadAgencies = StringUtils.trimToEmpty(asmt.getOrganization());
  		String contacts = StringUtils.trimToEmpty(asmt.getContact());
  		String partnering = StringUtils.trimToEmpty(asmt.getPartners());
  		String url = StringUtils.trimToEmpty(asmt.getUrl());
  		
  		
        PdfPTable table = new PdfPTable(2);
        try
        {
          table.setWidths(new int[]{1, 4});
        }
        catch (Exception e)
        {
      	  logger.fatal("Could not setWidths???" + e );
        }
        table.setSplitRows(false);
        
        PdfPCell nameTitle = new PdfPCell(new Phrase("Name")); 
        
        if (StringUtils.isNotBlank(acronym)) name = name + " (" + acronym + ")";
        PdfPCell nameCell = new PdfPCell(new Phrase(name));
        
        nameTitle.setBackgroundColor(BaseColor.CYAN);  nameCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        
        table.addCell(nameTitle);  table.addCell(nameCell);          		          		
  		
  		if (StringUtils.isNotBlank(leadAgencies))
  		{
  		  table.addCell(new PdfPCell(new Phrase("Lead Agencies")));  table.addCell(new PdfPCell(new Phrase(leadAgencies)));
  		}

  		if (StringUtils.isNotBlank(contacts))
  		{
  		  table.addCell(new PdfPCell(new Phrase("Contacts")));  table.addCell(new PdfPCell(new Phrase(contacts)));
  		}
        
  		if (StringUtils.isNotBlank(partnering))
  		{
  		  table.addCell(new PdfPCell(new Phrase("Partnering Agencies")));  table.addCell(new PdfPCell(new Phrase(partnering)));
  		}


  	    // compile the types list
  		com.itextpdf.text.List types = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
  		if (asmt.isClimateScience()) 
  		{
  			ListItem item = new ListItem(getLabel("climateScience")); types.add(item);
  		}
  		if (asmt.isNeedsAndCapabilities()) 
  		{
  			ListItem item = new ListItem(getLabel("needsAndCapabilities")); types.add(item);
  		}
  		if (asmt.isNeeds()) 
  		{
  			ListItem item = new ListItem(getLabel("needs")); types.add(item);
  		}
  		if (asmt.isCapacity()) 
  		{
  			ListItem item = new ListItem(getLabel("capacity")); types.add(item);
  		}
  		if (asmt.isCapabilities()) 
  		{
  			ListItem item = new ListItem(getLabel("capabilities")); types.add(item);
  		}
  		if (asmt.isRiskVulnerability()) 
  		{
  			ListItem item = new ListItem(getLabel("riskVulnerability")); types.add(item);
  		}


  		if(types.size() > 0)
  		{
  		  PdfPCell typesCell = new PdfPCell(); typesCell.addElement(types);
  		  table.addCell(new PdfPCell(new Phrase("Types")));  table.addCell(typesCell);
  		}
  		
  		
        //Aoa
  		com.itextpdf.text.List aoa = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
  		if(asmt.isInternational())
  		{
  			ListItem item = new ListItem(getLabel("international")); aoa.add(item);
  		}
  		if(asmt.isNational())
  		{
  			ListItem item = new ListItem(getLabel("national")); aoa.add(item);
  		}
  		if(asmt.isRegional())
  		{
  			ListItem item = new ListItem(getLabel("regional")); aoa.add(item);
  		}
  		
  		if (aoa.size() > 0)
  		{
   		  PdfPCell aoaCell = new PdfPCell(); aoaCell.addElement(aoa);
   		  table.addCell(new PdfPCell(new Phrase("Area of Applicability")));  table.addCell(aoaCell);
  		}
  		
  		// focus area
  		com.itextpdf.text.List fa = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
  		if(asmt.isWater())
  		{
  			ListItem item = new ListItem(getLabel("water")); fa.add(item);
  		}
  		if(asmt.isCoastal())
  		{
  			ListItem item = new ListItem(getLabel("coastal")); fa.add(item);
  		}
  		if(asmt.isEcosystem())
  		{
  			ListItem item = new ListItem(getLabel("ecosystem")); fa.add(item);
  		}
  		
  		if (fa.size() > 0)
  		{
   		  PdfPCell faCell = new PdfPCell(); faCell.addElement(fa);
   		  table.addCell(new PdfPCell(new Phrase("Focus Area")));  table.addCell(faCell);
  		}

       
  		//region
  		com.itextpdf.text.List regions = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
  		if (asmt.isCentralNorthPacific())
  		{
  			ListItem item = new ListItem(getLabel("centralNorthPacific")); regions.add(item);
  		}
  		if (asmt.isStateOfHawaii())
  		{
  			ListItem item = new ListItem(getLabel("stateOfHawaii")); regions.add(item);
  		}
  		if (asmt.isNorthWestHawaiianIslands())
  		{
  			ListItem item = new ListItem(getLabel("northWesternHawaiianIslands")); regions.add(item);
  		}
  		if (asmt.isPacificRemoteIslands())
  		{
  			ListItem item = new ListItem(getLabel("pacificRemoteIslands")); regions.add(item);
  		}
  		if (asmt.isWesternNorthPacific())
  		{
  			ListItem item = new ListItem(getLabel("westernNorthPacific")); regions.add(item);
  		}
  		if (asmt.isCnmi())
  		{
  			ListItem item = new ListItem(getLabel("cnmi")); regions.add(item);
  		}
  		if (asmt.isFsm())
  		{
  			ListItem item = new ListItem(getLabel("fsm")); regions.add(item);
  		}
  		if (asmt.isGuam())
  		{
  			ListItem item = new ListItem(getLabel("guam")); regions.add(item);
  		}
  		if (asmt.isPalau())
  		{
  			ListItem item = new ListItem(getLabel("palau")); regions.add(item);
  		}
  		if (asmt.isRmi())
  		{
  			ListItem item = new ListItem(getLabel("rmi")); regions.add(item);
  		}
  		if (asmt.isOtherWesternNorthPacific())
  		{
  			ListItem item = new ListItem(getLabel("otherWesternNorthPacific")); regions.add(item);
  		}
  		if (asmt.isSouthPacific())
  		{
  			ListItem item = new ListItem(getLabel("southPacific")); regions.add(item);
  		}
  		if (asmt.isAmericanSamoa())
  		{
  			ListItem item = new ListItem(getLabel("americanSamoa")); regions.add(item);
  		}
  		if (asmt.isAustralia())
  		{
  			ListItem item = new ListItem(getLabel("australia")); regions.add(item);
  		}
  		if (asmt.isCookIslands())
  		{
  			ListItem item = new ListItem(getLabel("cookIslands")); regions.add(item);
  		}
  		if (asmt.isFiji())
  		{
  			ListItem item = new ListItem(getLabel("fiji")); regions.add(item);
  		}
  		if (asmt.isFrenchPolynesia())
  		{
  			ListItem item = new ListItem(getLabel("frenchPolynesia")); regions.add(item);
  		}
  		if (asmt.isKiribati())
  		{
  			ListItem item = new ListItem(getLabel("kiribati")); regions.add(item);
  		}
  		if (asmt.isNewZealand())
  		{
  			ListItem item = new ListItem(getLabel("newZealand")); regions.add(item);
  		}
  		if (asmt.isPng())
  		{
  			ListItem item = new ListItem(getLabel("png")); regions.add(item);
  		}
  		if (asmt.isSamoa())
  		{
  			ListItem item = new ListItem(getLabel("samoa")); regions.add(item);
  		}
  		if (asmt.isSolomonIslands())
  		{
  			ListItem item = new ListItem(getLabel("solomonIslands")); regions.add(item);
  		}
  		if (asmt.isTonga())
  		{
  			ListItem item = new ListItem(getLabel("tonga")); regions.add(item);
  		}
  		if (asmt.isTuvalu())
  		{
  			ListItem item = new ListItem(getLabel("tuvalu")); regions.add(item);
  		}
  		if (asmt.isVanuatu())
  		{
  			ListItem item = new ListItem(getLabel("vanuatu")); regions.add(item);
  		}
  		if (asmt.isOtherSouthPacific())
  		{
  			ListItem item = new ListItem(getLabel("otherSouthPacific")); regions.add(item);
  		}
  		if (asmt.isPacificBasin())
  		{
  			ListItem item = new ListItem(getLabel("pacificBasin")); regions.add(item);
  		}
  		if (asmt.isGlobal())
  		{
  			ListItem item = new ListItem(getLabel("global")); regions.add(item);
  		}
  		
		
  		if (regions.size() > 0)
  		{
   		  PdfPCell rCell = new PdfPCell(); rCell.addElement(regions);
   		  table.addCell(new PdfPCell(new Phrase("Regions")));  table.addCell(rCell);
  		}
  		
  		
  		//status
  		com.itextpdf.text.List status = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
  		if (asmt.isCompleted())
  		{
  			ListItem item = new ListItem(getLabel("completed")); status.add(item);
  		}
  		if (asmt.isOngoing())
  		{
  			ListItem item = new ListItem(getLabel("ongoing")); status.add(item);
  		}
  		if (asmt.isPlanned())
  		{
  			ListItem item = new ListItem(getLabel("planned")); status.add(item);
  		}
  		if (asmt.isProposed())
  		{
  			ListItem item = new ListItem(getLabel("proposed")); status.add(item);
  		}
  		
  		if (status.size() > 0)
  		{
   		  PdfPCell sCell = new PdfPCell(); sCell.addElement(status);
   		  table.addCell(new PdfPCell(new Phrase("Status")));  table.addCell(sCell);
  		}
  		
  		
  		
  		
  		if (StringUtils.isNotBlank(description))
  		{
  		  table.addCell(new PdfPCell(new Phrase("Description")));  table.addCell(new PdfPCell(new Phrase(description)));
  		}

  		if (StringUtils.isNotBlank(url))
  		{
    	  Anchor link = new Anchor(StringUtils.trimToEmpty(url)); link.setReference(StringUtils.trimToEmpty(url));
  		  table.addCell(new PdfPCell(new Phrase("Url")));  table.addCell(new PdfPCell(link));
  		}

  		return table;

  }

  private String getLabel (String varName)
  {
	   String key = varName + "-label";
	   String value = "";
	   if (messages.contains(key)) value = messages.get(key);
	   else value = TapestryInternalUtils.toUserPresentable(varName);
	   return StringUtils.trimToEmpty(value);
  }
  private com.itextpdf.text.Image getLogo()
  {
	  java.awt.Image awtImage = Toolkit.getDefaultToolkit().createImage(logoAsset.getResource().toURL());
	  try
	  {
	    com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(awtImage, null);
	    logo.scalePercent(50);
	    return logo;
	  }
	  catch (Exception e)
	  {
		 logger.warn("Could not generate logo " + e);
	  }
	  
     return null;
  }
  private String getHeader(String prefix)
  {
	      DateFormat formatter = new SimpleDateFormat
                 ("EEE MMM dd HH:mm:ss zzz yyyy");
             Date date = new Date(System.currentTimeMillis());
             TimeZone eastern = TimeZone.getTimeZone("Pacific/Honolulu");
             formatter.setTimeZone(eastern);
             
     return prefix + formatter.format(date); 
  }
  
}