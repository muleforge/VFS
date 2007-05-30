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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.ConnectException;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;


/**
 * TODO - not sure this is the best way to go
 */
public class VFSReceiver extends PollingMessageReceiver
{

    private static Map checksumMap = Collections.synchronizedMap(new HashMap());
    private FileObject dirObject;
    private String fileExtension;
    private boolean includeSubfolders;

 

    public VFSReceiver(org.mule.transports.vfs.VFSConnector connector, UMOComponent component,
                       UMOEndpoint endpoint)
            throws InitialisationException
    {
        //super(connector, component, endpoint, connector.getTrigger());
        super(connector, component, endpoint, new Long(((VFSConnector)connector)
                .getPollingFrequency()));

        fileExtension = connector.getFileExtension();
        includeSubfolders = connector.isIncludeSubFolders();
    }

    public void poll()
    {
        try
        {
            dirObject.refresh();
            
            if (dirObject.exists())
            {
                if (dirObject.getType() == FileType.FOLDER)
                {
                    FileObject[] files = null;
                    files = dirObject.findFiles(new FileSelector()
                    {
                        public boolean includeFile(FileSelectInfo fileInfo) throws java.lang.Exception
                        {
                            if ((!fileInfo.getFile().isHidden()) && (fileInfo.getFile().getType() != FileType.FOLDER))
                            {
                                if (fileExtension.equals("*"))
                                {
                                    return true;
                                } else
                                {
                                    return (fileInfo.getFile().getName().getPath().endsWith(fileExtension));
                                }
                            } else
                            {
                                return false;
                            }
                        }

                        public boolean traverseDescendents(FileSelectInfo fileInfo) throws java.lang.Exception
                        {
                            if (includeSubfolders)
                            {
                                return (!fileInfo.getFile().isHidden());
                            } else
                            {
                                return ((!fileInfo.getFile().isHidden()) && fileInfo.getDepth() < 2);
                            }
                        }
                    });
                    
                    
                    
                    boolean autoDelete = ((VFSConnector) connector).isAutoDelete();
                    for (int i = 0; i < files.length; i++)
                    {
                    	if (autoDelete)
                    	{
                    		processFile(files[i]);
                    		if (!files[i].delete())
                    		{
                    			throw new MuleException(new Message("vfs", 3, files[i].getName()));
                    		}
                    	}
                    	else if (hasChanged(files[i]))
                        {
                            processFile(files[i]);
                        }
                    }
                    
                }
            }
        }
        catch (Exception e)
        {
        	Exception handled = e;
        	if(e instanceof FileSystemException)
        	{
        		handled = new ConnectException(e,this);
        	}
            handleException(handled);
        }
    }

    private void processFile(FileObject fileObject) throws UMOException
    {
        UMOMessageAdapter msgAdapter = connector.getMessageAdapter(fileObject);
        msgAdapter.setProperty(VFSConnector.PROPERTY_ORIGINAL_FILENAME, fileObject.getName().getPath());
        UMOMessage message = new MuleMessage(msgAdapter);
        routeMessage(message, endpoint.isSynchronous());
    }

    public void doConnect() throws Exception
    {
       dirObject = ((VFSConnector) connector).resolveFile(endpoint.getEndpointURI().getAddress());
       if (!dirObject.exists())
       {
		   throw new ConnectException(new Message(Messages.FILE_X_DOES_NOT_EXIST,
                   dirObject.getName()), this);
       }
	   if (!dirObject.isReadable())
	   {
		   throw new ConnectException(new Message(Messages.FILE_X_DOES_NOT_EXIST,
                   dirObject.getName()), this);
	   }
    }

    public void doDisconnect() throws Exception
    {
        if (dirObject != null)
        {
        	dirObject.close();
        	((VFSConnector) connector).closeFileSystem(dirObject);
        	dirObject = null;
        }
    }

    protected boolean hasChanged(FileObject fileObject)
    {
        boolean changed = false;
        String key = fileObject.getName().getPath();
        long checksum = 0;
        if (checksumMap.containsKey(key))
        {
            checksum = ((Long) checksumMap.get(key)).longValue();
        }
        long newChecksum = 0;
        CheckedInputStream checkedInputStream = null;
        try
        {
            InputStream inputStream = fileObject.getContent().getInputStream();
            checkedInputStream = new CheckedInputStream(inputStream, new Adler32());
            int bufferSize=1;
            if (inputStream.available()>0)
            {
            	bufferSize = inputStream.available();
            }
            byte[] buffer = new byte[bufferSize];
            while (checkedInputStream.read(buffer) > -1)
            {
                ;
            }
            newChecksum = checkedInputStream.getChecksum().getValue();
            if (newChecksum != checksum)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("calculated a new checksum of " + newChecksum);
                }
                checksumMap.put(key, new Long(newChecksum));
                changed = true;
            }
        }
        catch (IOException e)
        {
            connector.handleException(e);
        }
        return changed;
    }
}
