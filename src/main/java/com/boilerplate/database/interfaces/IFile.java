package com.boilerplate.database.interfaces;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IFile {
	/**
	 * Saves a multipart file uploaded via http
	 * 
	 * @param file
	 *            The file
	 * @return The file name on disk
	 * @throws IOException
	 *             If there is any error accessing the file
	 */
	public String saveFile(MultipartFile file) throws IOException;

	public String getPreSignedS3URL(String id);
}
