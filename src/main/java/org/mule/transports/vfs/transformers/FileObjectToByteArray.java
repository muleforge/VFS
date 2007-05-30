/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transports.vfs.transformers;

import org.apache.commons.vfs.FileObject;
import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.InputStream;

/**
 * TODO
 */
public class FileObjectToByteArray extends AbstractTransformer
{

    public FileObjectToByteArray()
    {
        registerSourceType(FileObject.class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object msg, String encoding) throws TransformerException
    {
        if (msg instanceof FileObject)
        {
            try
            {
                InputStream in = ((FileObject) msg).getContent().getInputStream();
                byte[] content = new byte[(int) ((FileObject) msg).getContent().getSize()];
                in.read(content);
                return content;
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        } else
        {
            throw new TransformerException(Message.createStaticMessage("Message is not an instance of org.apache.commons.vfs.FileObject"), this);
        }
    }
}
