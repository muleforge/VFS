/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transports.vfs;

import java.util.ArrayList;
import java.util.Date;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class VfsSftpFunctionalTestCase extends FunctionalTestCase
{

	private static final long TIMEOUT = 30000;
	
	private ArrayList sendFiles;
	private ArrayList receiveFiles;
	
	
    protected String getConfigResources()
    {
        return "mule-vfs-test-sftp-config.xml";
    }


    
    public void testSendAndReceiveSingleFile() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("file created on " + new Date());
    	
    	sendAndReceiveFiles();
    }

  
    public void testSendAndReceiveMultipleFiles() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());    	
    	
    	
    	sendAndReceiveFiles();
    }   
 
    
    //Test Mule-1477
    public void testSendAndReceiveEmptyFile() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("");
    	
    	sendAndReceiveFiles();
    }
 
    public void sendAndReceiveFiles() throws Exception
    {
        MuleClient client = new MuleClient();  
                
        for( int i = 0; i < sendFiles.size(); i++)
        {
            client.send("vm://test.upload",(String) sendFiles.get(i),null);        	
        }
   
   
        
        UMOMessage m;
        
        receiveFiles = new ArrayList();
        
        while( (m = client.receive("vm://test.download",TIMEOUT)) != null)
        {
        	
        	String fileText = m.getPayloadAsString();
            assertNotNull(fileText);
        	
        	if( sendFiles.contains(fileText))
        	{
                receiveFiles.add(fileText);	
        	}      	
        }

        //This makes sure we received the same number of files we sent, and that
        //the content was a match (since only matched content gets on the 
        //receiveFiles ArrayList)
        assertTrue( sendFiles.size() == receiveFiles.size() );
        
      
    }
    
  


}
