package org.ideademo.asmts.pages.pdf;

import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.StreamResponse;


public class Index {
	@InjectPage
	private org.ideademo.asmts.pages.Index index;
	
	public StreamResponse onActivate()
    {
		return index.onSelectedFromPdf();
    }
}