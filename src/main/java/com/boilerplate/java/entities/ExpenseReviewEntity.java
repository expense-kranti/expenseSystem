package com.boilerplate.java.entities;

import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.fasterxml.jackson.annotation.*;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class ExpenseReviewEntity extends BaseEntity {

	/**
	 * This is the status to be assigned in string format
	 */
	@ApiModelProperty(value = "This is the string equivalent of expense status", required = true, notes = "This is the id of the expnese being approved or rejected")
	private String statusString;

	/**
	 * This is the status
	 */
	@JsonIgnore
	@ApiModelProperty(value = "This is the status of the expense", required = true, notes = "The status determines what action needs to be takes on expense")
	private ExpenseStatusType status;

	/**
	 * These are the approver comments
	 */
	@ApiModelProperty(value = "This is the comments provided by the approver", required = true, notes = "The comments are mandatory if expense is being rejected")
	private String approverComments;

	/**
	 * This is the expense Id
	 */
	@ApiModelProperty(value = "This is the id of the expnese being approved or rejected", required = true, notes = "This is the id of the expnese being approved or rejected")
	private String expenseId;

	/**
	 * This method is used to get status
	 * 
	 * @return
	 */
	public ExpenseStatusType getStatus() {
		return status;
	}

	/**
	 * This method is used to set status
	 * 
	 * @param status
	 */
	public void setStatus(ExpenseStatusType status) {
		this.status = status;
	}

	/**
	 * This method is used to get approver comments
	 * 
	 * @return
	 */
	public String getApproverComments() {
		return approverComments;
	}

	/**
	 * This method is used to set approver comments
	 * 
	 * @param approverComments
	 */
	public void setApproverComments(String approverComments) {
		this.approverComments = approverComments;
	}

	/**
	 * This method is used to expense id
	 * 
	 * @return
	 */
	public String getExpenseId() {
		return expenseId;
	}

	/**
	 * This method used to set expense id
	 * 
	 * @param expenseId
	 */
	public void setExpenseId(String expenseId) {
		this.expenseId = expenseId;
	}

	/**
	 * This method is used to get status of the expense
	 * 
	 * @return
	 */
	public String getStatusString() {
		return statusString;
	}

	/**
	 * This method is used to set status of the expense
	 * 
	 * @param statusString
	 */
	public void setStatusString(String statusString) throws ValidationFailedException {
		this.statusString = statusString;
		for (ExpenseStatusType status : ExpenseStatusType.values())
			if (statusString.equalsIgnoreCase(String.valueOf(status)))
				this.setStatus(status);

	}

	/**
	 * @see Base.validate
	 */
	@Override
	public boolean validate() throws ValidationFailedException {
		// validate that none of the fields are null or empty
		if (isNullOrEmpty(this.getExpenseId()) || this.getStatus() == null)
			throw new ValidationFailedException("ExpenseReviewEntity",
					"Either expense id is null or status is null or has an invalid value", null);
		// check if status is only finance approve or finance rejected
		if (!this.status.equals(ExpenseStatusType.Finance_Approved))
			if (!this.status.equals(ExpenseStatusType.Finance_Rejected))
				throw new ValidationFailedException("ExpenseStatusType",
						"Finance can either reject an expense or approve it, any other status is not allowed", null);
		return true;
	}

	/**
	 * @see Base.transformToInternal
	 */
	@Override
	public BaseEntity transformToInternal() {
		return this;
	}

	/**
	 * @see Base.transformToExternal
	 */
	@Override
	public BaseEntity transformToExternal() {
		return this;
	}

}
