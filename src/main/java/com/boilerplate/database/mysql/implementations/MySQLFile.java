package com.boilerplate.database.mysql.implementations;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;

import org.joda.time.DateTime;

import com.boilerplate.database.interfaces.IFilePointer;
import com.boilerplate.exceptions.rest.NotFoundException;
import com.boilerplate.java.collections.BoilerplateList;
import com.boilerplate.java.collections.BoilerplateMap;
import com.boilerplate.java.entities.FileEntity;

public class MySQLFile extends MySQLBaseDataAccessLayer implements IFilePointer  {

	
	/**
	 * @throws Exception 
	 * @see IFilePointer.mySqlSaveFile
	 */
	@Override
	public void mySqlSaveFile(FileEntity file) throws Exception {
		// Set creation time to current time
		file.setCreationDate(Date.valueOf(LocalDate.now()));
		file.setId(null);
		// save file in MySql Database
		super.create(file);
		
	}

	@Override
	public FileEntity save(FileEntity fileEntity) throws Exception {
		
		// save file in MySql Database
		super.create(fileEntity);
		return fileEntity;
	}

	@Override
	public FileEntity getFilePointerById(String id) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileEntity updateMetaData(String id, BoilerplateMap<String, String> metaDataMap) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoilerplateList<FileEntity> getAllFiles(String userId, String organizationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoilerplateList<FileEntity> getFiles(String organizationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BoilerplateList<FileEntity> getAllFilesOnMasterTag(String id, String fileMasterTag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addInRedisSet(FileEntity fileEntity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> fetchUserFileAndAddInQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteItemFromRedisUserFileSet(String fileName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addInRedisSet(String fileName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getAllFileKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	

}
