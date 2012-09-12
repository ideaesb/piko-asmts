package org.ideademo.asmts.pages.asmt;


import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Property;

import org.ideademo.asmts.entities.Asmt;


public class ViewAsmt
{
	
  @PageActivationContext 
  @Property
  private Asmt entity;
  
  
  void onPrepareForRender()  {if(this.entity == null){this.entity = new Asmt();}}
  void onPrepareForSubmit()  {if(this.entity == null){this.entity = new Asmt();}}
}
