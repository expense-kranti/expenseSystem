package com.boilerplate.database.interfaces;

import java.util.List;
import java.util.Map;

import com.boilerplate.exceptions.rest.BadRequestException;
import com.boilerplate.java.entities.ExpenseEntity;
import com.boilerplate.java.entities.ExpenseHistoryEntity;
import com.boilerplate.java.entities.ExpenseStatusType;
import com.boilerplate.java.entities.FetchExpenseEntity;
import com.boilerplate.java.entities.FileMappingEntity;
import com.boilerplate.java.entities.UserRoleType;

/**
 * This interface has methods for CRUD operations of expense entity in MySQL
 * database
 * 
 * @author ruchi
 *
 */
public interface IExpense {

	/**
	 * this method is used to save a new expense in the system
	 * 
	 * @param expenseEntity
	 *            this is the expense entity to be saved
	 * @return Saved expense entity
	 * @throws Exception
	 *             throw this exception if some exception occurs while saving
	 *             entity in database
	 */
	public ExpenseEntity createExpense(ExpenseEntity expenseEntity) throws Exception;

	/**
	 * This method is used to update expense
	 * 
	 * @param expenseEntity
	 *            this is the expense entity to be saved
	 * @return Saved expense entity
	 */
	public ExpenseEntity updateExpense(ExpenseEntity expenseEntity);

	/**
	 * This method is used to get an expense by id
	 * 
	 * @param id
	 *            This is the id of the expense
	 * @return This is the updated and saved expense entity
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 */
	public ExpenseEntity getExpense(String id) throws BadRequestException;

	/**
	 * This method is used to save expense history
	 * 
	 * @param expenseHistoryEntity
	 *            this is the expense history to be saved
	 * @return
	 * @throws Exception
	 *             throw this exception if any exception occurs hile saving
	 *             expense history
	 */
	public ExpenseHistoryEntity saveExpenseHistory(ExpenseHistoryEntity expenseHistoryEntity) throws Exception;

	/**
	 * This method is used to get list of expenses for a given user
	 * 
	 * @param fetchExpenseEntity
	 *            This entity contain user id, date range and status for
	 *            filtration
	 * @return List of expenses
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 */
	public List<ExpenseEntity> getExpenses(FetchExpenseEntity fetchExpenseEntity) throws BadRequestException;

	/**
	 * This method is used to fetch list of expenses filed under an approver
	 * 
	 * @param approverId
	 *            this is the id of the approver
	 * @return List of expenses
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 */
	public List<ExpenseEntity> getExpensesForApprover(String approverId) throws BadRequestException;

	/**
	 * This method is used to get file mappings
	 * 
	 * @param expenseIds
	 *            This is the list of expense ids
	 * @return List of file mappings
	 * @throws BadRequestException
	 *             Throw this exssception if user sends a bad request
	 */
	public List<FileMappingEntity> getFileMappingsForExpenses(String expenseIds) throws BadRequestException;

	/**
	 * This method is used to get expenses for finance
	 * 
	 * @param status
	 *            This is status
	 * @return List<Map<String, Object>>
	 * @throws BadRequestException
	 *             Throw this exception if user sends bad request
	 */
	public List<Map<String, Object>> getUserAmountsForFinance(String status) throws BadRequestException;

	/**
	 * This method is used to save a list of expenses
	 * 
	 * @param expenses
	 *            This is the list of expenses
	 * @throws Exception
	 *             Throw this exception if any exception occurs while saving
	 *             expense
	 */
	public void saveExpenseList(List<ExpenseEntity> expenses) throws Exception;

	/**
	 * This method is used o delete expense when exception occurs while saving
	 * file mapping
	 * 
	 * @param expenseEntity
	 *            Thi is the expense entity to be deleted
	 */
	public void deleteExpense(ExpenseEntity expenseEntity);

	/**
	 * This method is used to get all active expenses
	 * 
	 * @return List of expenses
	 * @throws BadRequestException
	 */
	public List<ExpenseEntity> getExpensesForSuper() throws BadRequestException;

	/**
	 * This method is used to get status by expense
	 * 
	 * @param status
	 *            This is the status
	 * @return List of expenses
	 * @throws BadRequestException
	 *             throw this exception if user sends bad request
	 */
	public List<ExpenseEntity> getExpensesByStatus(ExpenseStatusType status) throws BadRequestException;
}
