package org.mule.samples.vfs;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;

public class FileSystemOptionsFactory
{
	public FileSystemOptions getFsOptions() throws FileSystemException 
	{ 
		FileSystemOptions fsopts=new FileSystemOptions(); 
	  
		SftpFileSystemConfigBuilder builder=SftpFileSystemConfigBuilder.getInstance(); 
	  
		builder.setStrictHostKeyChecking(fsopts, "no");
	  
		return fsopts; 
	}
}
