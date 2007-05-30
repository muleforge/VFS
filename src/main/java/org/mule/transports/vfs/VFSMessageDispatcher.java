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

import org.apache.commons.vfs.FileFilter;
import org.apache.commons.vfs.FileFilterSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSystemException;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.ConnectException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.IOException;
import java.io.OutputStream;

/**
 * TODO
 */
public class VFSMessageDispatcher extends AbstractMessageDispatcher
{

    private VFSConnector connector;
    private FileObject dirObject;


    public VFSMessageDispatcher(UMOImmutableEndpoint umoImmutableEndpoint)
    {
        super(umoImmutableEndpoint);
        connector = (VFSConnector) umoImmutableEndpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint umoImmutableEndpoint) throws Exception
    {
        dirObject = connector.resolveFile(VFSConnector.resolveUri(umoImmutableEndpoint.getEndpointURI().getAddress()));
    }

    protected void doDisconnect() throws Exception
    {
        if (dirObject != null)
        {
        	dirObject.close();
        	connector.closeFileSystem(dirObject);
        	dirObject = null;
        }
    }

    protected UMOMessage doReceive(UMOImmutableEndpoint umoImmutableEndpoint, long l) throws Exception
    {
        UMOMessage result = null;
        if (connector.getSelectExpression() != null && (!connector.getSelectExpression().equals("")))
        {
            FileObject[] files = dirObject.findFiles(new FileFilterSelector(new FileFilter()
            {
                public boolean accept(FileSelectInfo fileInfo)
                {
                    return fileInfo.getFile().getName().getPath().matches(connector.getSelectExpression());
                }
            }));
            new MuleMessage(connector.getMessageAdapter(files));
        }
        return result;
    }

    protected void doDispose()
    {
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        try
        {
            Object data = event.getTransformedMessage();
            String filename = (String) event.getProperty(VFSConnector.PROPERTY_FILENAME);

            if (filename == null)
            {
                String outPattern = (String) event.getProperty(VFSConnector.PROPERTY_OUTPUT_PATTERN);
                if (outPattern == null)
                {
                    outPattern = connector.getOutputPattern();
                }
                filename = generateFilename(event, outPattern);
            }

            if (filename == null)
            {
                throw new IOException("Filename is null");
            }

            FileObject file = dirObject.resolveFile(filename);
            byte[] buf;
            if (data instanceof byte[])
            {
                buf = (byte[]) data;
            } else
            {
                buf = data.toString().getBytes();
            }

            logger.info("Writing file to: " + file.getURL());
            OutputStream outputStream = file.getContent().getOutputStream(connector.isOutputAppend());
            try
            {
                outputStream.write(buf);
            }
            finally
            {
                outputStream.close();
            }
        }
        catch (Exception e)
        {
        	Exception handled = e;
        	if(e instanceof FileSystemException)
        	{
        		handled = new ConnectException(e, this);
        	}
            getConnector().handleException(handled);
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    private String generateFilename(UMOEvent event, String pattern)
    {
        if (pattern == null)
        {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(event.getMessage(), pattern);
    }


}
