package com.boilerplate.java.entities;

import java.util.List;

import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * This is save role entity used to take input from API for a given user id with
 * a list of roles
 * 
 * @author ruchi
 *
 */
public class SaveRoleEntity extends BaseEntity {

	/**
	 * This is the default constructor
	 */
	public SaveRoleEntity() {
		super();
	}

	/**
	 * This is the parameterized constructor
	 * 
	 * @param userId
	 *            This is the id of user
	 * @param roles
	 *            tHIS IS THE LIST OF ROLES
	 */
	public SaveRoleEntity(String userId, List<UserRoleType> roles) {
		super();
		this.userId = userId;
		this.roles = roles;
	}

	/**
	 * This is the user id
	 */
	@ApiModelProperty(value = "This is the user id", required = true, notes = "This is the user id")
	private String userId;

	/**
	 * this is the list of roles for given user
	 */
	@ApiModelProperty(value = "This is the list of roles", required = true, notes = "This is the list of roles")
	private List<UserRoleType> roles;

	/**
	 * This method is used to get list user id
	 * 
	 * @return
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * This method is used to set user id
	 * 
	 * @param userId
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * This method is used to get roles
	 * 
	 * @return
	 */
	public List<UserRoleType> getRoles() {
		return roles;
	}

	/**
	 * This method is used to set roles
	 * 
	 * @param roles
	 */
	public void setRoles(List<UserRoleType> roles) {
		this.roles = roles;
	}

	/**
	 * @see BaseEntity.validate
	 */
	@Override
	public boolean validate() throws ValidationFailedException {
		// check if user id and role is not null or empty
		if (this.isNullOrEmpty(this.getUserId()))
			throw new ValidationFailedException("UserRoleEntity", "User id is null/empty", null);
		// check if role is null or empty
		if (this.getRoles().size() == 0)
			throw new ValidationFailedException("UserRoleEntity", "Role is null/empty", null);
		return true;
	}

	/**
	 * @see BaseEntity.transformToInternal
	 */
	@Override
	public BaseEntity transformToInternal() {
		return this;
	}

	/**
	 * @see BaseEntity.transformToExternal
	 */
	@Override
	public BaseEntity transformToExternal() {
		return this;
	}

}
