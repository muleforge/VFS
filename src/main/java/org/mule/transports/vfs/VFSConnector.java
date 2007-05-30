/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transports.vfs;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.mule.MuleException;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.SimpleFilenameParser;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 27, 2005
 * Time: 12:05:48 AM
 */
public class VFSConnector extends AbstractServiceEnabledConnector
{

    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_SELECT_EXPRESSION = "selectExpression";
    public static final String PROPERTY_FILE_EXTENSION = "fileExtension";
    public static final String PROPERTY_INCLUDE_SUBFOLDERS = "includeSubfolders";

//  private SVNUpdateClient updateClient;
//  private SVNUpdateClient commitClient;
//  private SVNUpdateClient wcClient;


    private String outputPattern;
    private FilenameParser filenameParser;
    private long pollingFrequency = 30000;
    private String selectExpression;
    private boolean includeSubFolders = false;
    private boolean outputAppend = false;
    private String fileExtension = "*";

    private boolean autoDelete = true;
    private FileSystemOptions fileSystemOptions=new FileSystemOptions();
    
    public VFSConnector()
    {
        filenameParser = new SimpleFilenameParser();
        
        super.registerSupportedProtocol("sftp");
        super.registerSupportedProtocol("ftp");        
        super.registerSupportedProtocol("jar");  
        super.registerSupportedProtocol("http");  
        super.registerSupportedProtocol("https");  
    }

    public String getProtocol()
    {
        return "VFS";
    }

	public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        String directory = endpoint.getEndpointURI().getAddress();
        
        FileObject dirObject = this.resolveFile(directory);
        Map props = endpoint.getProperties();

        //Override properties on the endpoint for the specific endpoint

        if (props != null)
        {
            String overrideFileExtension = (String) props.get(PROPERTY_FILE_EXTENSION);
            if (overrideFileExtension != null)
            {
                fileExtension = overrideFileExtension;
            }
            String overrideIncludeSubFolders = (String) props.get(PROPERTY_INCLUDE_SUBFOLDERS);
            if (overrideIncludeSubFolders != null)
            {
                includeSubFolders = new Boolean(overrideIncludeSubFolders).booleanValue();
            }
        }

        if (dirObject.getType() == FileType.FOLDER)
        {

            UMOMessageReceiver fileReceiver = new VFSReceiver(this, component, endpoint);
            dirObject.close();
            closeFileSystem(dirObject);
            return fileReceiver;
        } else
        {
        	dirObject.close();
        	closeFileSystem(dirObject);
            throw new MuleException(Message.createStaticMessage("Path is not a directory or does not exist"));
        }
    }

    protected FileObject resolveFile(String file) throws FileSystemException
	{
    	FileSystemManager fsm = this.createFileSystemManager();
    	FileObject fo = fsm.resolveFile(file,fileSystemOptions);
    	return fo;
	}
    
    private FileSystemManager createFileSystemManager() throws FileSystemException
    {
    	StandardFileSystemManager fsm = new StandardFileSystemManager();
    	fsm.init();
    	return fsm;
    }
    
    protected void closeFileSystem(FileObject file)
    {
    	FileSystem fs = file.getFileSystem();
    	FileSystemManager fsm = fs.getFileSystemManager();
    	fsm.closeFileSystem(fs);
    }
    
	public boolean isIncludeSubFolders()
    {
        return includeSubFolders;
    }

    public void setIncludeSubFolders(boolean includeSubFolders)
    {
        this.includeSubFolders = includeSubFolders;
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public String getOutputPattern()
    {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern)
    {
        this.outputPattern = outputPattern;
    }

    public boolean isOutputAppend()
    {
        return outputAppend;
    }

    public void setOutputAppend(boolean outputAppend)
    {
        this.outputAppend = outputAppend;
    }

    public FilenameParser getFilenameParser()
    {
        return filenameParser;
    }

    public void setFilenameParser(FilenameParser filenameParser)
    {
        this.filenameParser = filenameParser;
    }

    public String getSelectExpression()
    {
        return selectExpression;
    }

    public void setSelectExpression(String selectExpression)
    {
        this.selectExpression = selectExpression;
    }

    public String getFileExtension()
    {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension)
    {
        this.fileExtension = fileExtension;
    }
    public boolean isAutoDelete()
    {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        this.autoDelete = autoDelete;
        if (!autoDelete)
        {
            if (serviceOverrides == null)
            {
                serviceOverrides = new Properties();
            }
            if (serviceOverrides.getProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER) == null)
            {
                serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER,
                    VFSMessageAdapter.class.getName());
            }
        }
    }

	public FileSystemOptions getFileSystemOptions()
	{
		return fileSystemOptions;
	}

	public void setFileSystemOptions(FileSystemOptions fileSystemOptions)
	{
		this.fileSystemOptions = fileSystemOptions;
	}

	protected static String resolveUri(String address)
	{
		if (address.startsWith("//")) {
			address = address.substring(2);
		}
		if (address.startsWith("/")) {
			address = address.substring(1);
		}
		int i = address.indexOf("?");
		if (i > -1) {
			address = address.substring(0, i);
		}
		return address;
	}


}
