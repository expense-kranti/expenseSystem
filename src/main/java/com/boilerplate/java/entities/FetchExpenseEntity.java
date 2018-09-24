package com.boilerplate.java.entities;

import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class FetchExpenseEntity extends BaseEntity {

	/**
	 * This is the start date for expense filtering
	 */
	@ApiModelProperty(value = "This is the start date for the filtering of expenses", required = true, notes = "This is the start date for the filtering of expenses")
	private String startDate;

	/**
	 * this is the end date for expense filtering
	 */
	@ApiModelProperty(value = "This is the end date for the filtering of expenses", required = true, notes = "This is the end date for the filtering of expenses")
	private String endDate;

	/**
	 * This is the type expenses to be fetched
	 */
	@ApiModelProperty(value = "This is the status of the expense", required = true, notes = "This is the status of the expense")
	private ExpenseStatusType expenseType;

	/**
	 * This is the string for enum ExpenseStatusType
	 */
	@ApiModelProperty(value = "This is the string equivalent of the expense status type", required = true, notes = "This is the string equivalent of the expense status type")
	private String statusString;

	/**
	 * This method is used get start date
	 * 
	 * @return
	 */
	public String getStartDate() {
		return startDate;
	}

	/**
	 * This method is used to set start date
	 * 
	 * @param startDate
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	/**
	 * This method is used to get end date
	 * 
	 * @return
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * This method is used to set end date
	 * 
	 * @param endDate
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * This method is used to get expense type
	 * 
	 * @return
	 */
	public ExpenseStatusType getExpenseType() {
		return expenseType;
	}

	/**
	 * This method is used to set expense type
	 * 
	 * @param expenseType
	 */
	public void setExpenseType(ExpenseStatusType expenseType) {
		this.expenseType = expenseType;
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

	/**
	 * @see BaseEntity.validate
	 */
	@Override
	public boolean validate() throws ValidationFailedException {
		// check if start date is given then end date is mandatory and vice
		// versa
		if (isNullOrEmpty(this.getStartDate())) {
			if (!isNullOrEmpty(this.getEndDate()))
				throw new ValidationFailedException("FetchExpenseEntity",
						"Either none or both the dates should be given", null);
		}
		if (isNullOrEmpty(this.getEndDate())) {
			if (!isNullOrEmpty(this.getStartDate()))
				throw new ValidationFailedException("FetchExpenseEntity",
						"Either none or both the dates should be given", null);
		}
		return true;
	}

}
