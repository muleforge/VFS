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

import org.mule.providers.AbstractMessageAdapter;
import org.apache.commons.vfs.FileObject;

import java.io.InputStream;


public class VFSMessageAdapter extends AbstractMessageAdapter
{
    private FileObject[] files;
    private FileObject file;
    private Object payload;

    public VFSMessageAdapter(FileObject[] files)
    {
        if (files.length == 1)
        {
            this.file = files[0];
            payload = this.file;
        } else
        {
            this.files = files;
            payload = this.files;
        }
    }

    public VFSMessageAdapter(FileObject file)
    {
        this.file = file;
        payload = this.file;
    }

    public String getPayloadAsString(String encoding) throws Exception
    {
        StringBuffer stringBuffer = new StringBuffer();
        byte[] buffer = null;
        if (payload instanceof FileObject[])
        {
            for (int i = 0; i < files.length; i++)
            {
                InputStream inputStream = files[i].getContent().getInputStream();
                buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                stringBuffer.append(files[i].getName().getPath());
                stringBuffer.append(": \n");
                stringBuffer.append(new String(buffer, encoding));
            }
        } else
        {
            InputStream inputStream = file.getContent().getInputStream();
            buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            stringBuffer.append(new String(buffer, encoding));
            inputStream.close();
        }
        return stringBuffer.toString();
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        byte[] buffer = null;
        if (payload instanceof FileObject[])
        {
            int bufferLength = 0;
            for (int i = 0; i < files.length; i++)
            {
                InputStream inputStream = files[i].getContent().getInputStream();
                bufferLength += inputStream.available();
                inputStream.close();
            }
            buffer = new byte[bufferLength];
            int start = 0;
            for (int i = 0; i < files.length; i++)
            {
                InputStream inputStream = files[i].getContent().getInputStream();
                int length = inputStream.available();
                inputStream.read(buffer, start, length);
                inputStream.close();
                start += length;
            }
        } else
        {
            InputStream inputStream = file.getContent().getInputStream();
            buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
        }
        return buffer;
    }

    public Object getPayload()
    {
        return payload;
    }
}
