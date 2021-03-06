package com.boilerplate.java.controllers;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boilerplate.exceptions.rest.BadRequestException;
import com.boilerplate.exceptions.rest.NotFoundException;
import com.boilerplate.exceptions.rest.UnauthorizedException;
import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.boilerplate.java.entities.ExpenseEntity;
import com.boilerplate.java.entities.ExpenseListViewEntity;
import com.boilerplate.java.entities.ExpenseReportEntity;
import com.boilerplate.java.entities.ExpenseReviewEntity;
import com.boilerplate.java.entities.FetchExpenseEntity;
import com.boilerplate.service.interfaces.IExpenseService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * This controller has APIs for expense CRUD operations and other expense
 * related operations
 * 
 * @author ruchi
 *
 */
@Api(description = "This api has controllers for expense CRUD operations", value = "Expense API's", basePath = "/expense")
@Controller
public class ExpenseController extends BaseController {

	/**
	 * This is the autowired instance of IExpenseService
	 */
	@Autowired
	IExpenseService expenseService;

	/**
	 * This API is used to create a new expense in the system
	 * 
	 * @param expenseEntity
	 *            this is the Expense entity
	 * @return Saved expense entity
	 * @throws NotFoundException
	 *             Throw this exception if entity not found
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 * @throws Exception
	 *             Throw this exception if any exception occurs wile
	 *             saving/updating data in MySQL
	 */
	@ApiOperation(value = "Creates a new expense entity in the system", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/expense", method = RequestMethod.POST)
	public @ResponseBody ExpenseEntity createExpense(@RequestBody ExpenseEntity expenseEntity)
			throws ValidationFailedException, BadRequestException, NotFoundException, Exception {
		// call the business layer
		return expenseService.createExpense(expenseEntity);
	}

	/**
	 * This API is used to update an existing expense in the system
	 * 
	 * @param expenseEntity
	 *            this is the Expense entity
	 * @return Saved expense entity
	 * @throws NotFoundException
	 *             Throw this exception if entity not found
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 * @throws Exception
	 *             Throw this exception if any exception occurs wile
	 *             saving/updating data in MySQL
	 * 
	 */
	@ApiOperation(value = "Updates an expense entity in the system", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/expense", method = RequestMethod.PUT)
	public @ResponseBody ExpenseEntity updateExpense(@RequestBody ExpenseEntity expenseEntity)
			throws BadRequestException, ValidationFailedException, NotFoundException, Exception {
		// call the business layer
		return expenseService.updateExpense(expenseEntity);
	}

	/**
	 * This API is used to get an expense by its id
	 * 
	 * @param id
	 *            This is the id of the expense
	 * @return
	 * @throws UnauthorizedException
	 *             Throw this exception if user is not authorized to view this
	 *             expense
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if any validation fails
	 * @throws NotFoundException
	 *             Throw this exception if expense not found
	 */
	@ApiOperation(value = "Fetches an expense entity from the system", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/expense", method = RequestMethod.GET)
	public @ResponseBody ExpenseEntity getExpenseById(@RequestParam String id)
			throws ValidationFailedException, NotFoundException, BadRequestException, UnauthorizedException {
		// call the business layer
		return expenseService.getExpenseById(id);
	}

	/**
	 * This API is used to get expenses for a given user
	 * 
	 * @param userId
	 *            this is the user id for which expenses need o be fetched
	 * @return List of expenses
	 * @throws NotFoundException
	 *             Throw this exception if entity not found
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 * @throws ParseException
	 *             Throw this exception if parse exception occurs while parsing
	 *             date
	 */
	@ApiOperation(value = "Gets expenses for a given user", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/getExpensesForEmployee", method = RequestMethod.POST)
	public @ResponseBody List<ExpenseEntity> getExpensesForEmployee(@RequestBody FetchExpenseEntity fetchExpenseEntity)
			throws ValidationFailedException, NotFoundException, BadRequestException, ParseException {
		// call the business layer
		return expenseService.getExpensesForUser(fetchExpenseEntity);
	}

	/**
	 * This API is used to get expenses for approver/Super approvers
	 * 
	 * @param approverId
	 *            This is the id of the approver
	 * @param role
	 *            This is the role of the approver
	 * @return List of expenses
	 * @throws NotFoundException
	 *             Throw this exception if entity not found
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 */
	@ApiOperation(value = "Gets expenses for a given approver/super approver", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/getExpensesForApprover", method = RequestMethod.GET)
	public @ResponseBody List<ExpenseEntity> getExpensesForApprover()
			throws NotFoundException, ValidationFailedException, BadRequestException {
		// call the business layer
		return expenseService.getExpensesForApproval();
	}

	/**
	 * This API is used to get list of expenses for approver/super approver
	 * 
	 * @return List of expenses
	 * @throws NotFoundException
	 *             Throw this exception if no expenses are found
	 * @throws ValidationFailedException
	 *             Throw this exception if validation fails
	 * @throws BadRequestException
	 *             Throw this exception if user ends bad request
	 */
	@ApiOperation(value = "Gets expenses for a given approver/super approver", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/getExpenseListForApprover", method = RequestMethod.GET)
	public @ResponseBody List<ExpenseListViewEntity> getExpenseListForApproval()
			throws NotFoundException, ValidationFailedException, BadRequestException {
		// call the business layer
		return expenseService.getExpenseListForApprovers();
	}

	/**
	 * This API is used to approver expenses by approver/super approvers
	 * 
	 * @param expenseId
	 *            This is the id of the expense to be approved
	 * @return Updated expense entity
	 * @throws NotFoundException
	 *             Throw this exception if entity not found
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 * @throws Exception
	 *             Throw this exception if any exception occurs wile
	 *             saving/updating data in MySQL
	 */
	@ApiOperation(value = "Approve an expense", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/approveForApprovers", method = RequestMethod.POST)
	public @ResponseBody ExpenseEntity approveExpenseForApprover(@RequestBody ExpenseReviewEntity expenseReviewEntity)
			throws ValidationFailedException, BadRequestException, NotFoundException, Exception {
		// call the business layer
		return expenseService.approveExpenseForApprover(expenseReviewEntity);
	}

	/**
	 * This api is used to get list for expenses for finance
	 * 
	 * @return List of reports
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws NotFoundException
	 *             Throw this exception if entity is not found
	 * @throws ValidationFailedException
	 */
	@ApiOperation(value = "Gets expenses for finance", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/getExpensesForFinance", method = RequestMethod.GET)
	public @ResponseBody List<ExpenseEntity> getExpensesForFinance(@RequestParam String status)
			throws BadRequestException, NotFoundException, ValidationFailedException {
		// call the business layer
		return expenseService.getExpensesForFinance(status);
	}

	/**
	 * This API is used to approve/reject/move to ready for payment state by
	 * Finance
	 * 
	 * @param expenseReportEntity
	 *            This entity contains all the expenses to be updated
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws ValidationFailedException
	 *             Throw this exception if entity fails any validation rules
	 * @throws Exception
	 *             Throw this exception if any exception occurs wile
	 *             saving/updating data in MySQL
	 */
	@ApiOperation(value = "Approve an expense", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/approveForFinance", method = RequestMethod.POST)
	public @ResponseBody void approveExpenseForFinance(@RequestBody ExpenseReportEntity expenseReportEntity)
			throws BadRequestException, ValidationFailedException, Exception {
		// call the business layer
		expenseService.approveExpenseForFinance(expenseReportEntity);
	}

	/**
	 * This API is used to approve/reject a single expense by finance
	 * 
	 * @param expenseReviewEntity
	 *            This is the expense review entity
	 * @throws ValidationFailedException
	 *             Throw this exception if user sends an invalid request
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws NotFoundException
	 *             Throw this exception if expense not found
	 */
	@ApiOperation(value = "Approve/Reject a single expense by finance", notes = "Individual expense can be approved or rejcted by finance")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/expenseReviewByFinance", method = RequestMethod.POST)
	public @ResponseBody void expenseReviewByFinance(@RequestBody ExpenseReviewEntity expenseReviewEntity)
			throws ValidationFailedException, BadRequestException, NotFoundException {
		// call the business layer
		expenseService.expenseReviewByFinance(expenseReviewEntity);
	}

	/**
	 * This aPI is used to get expense reports in finance approved state/ready
	 * for payment state
	 * 
	 * @return List of expense reports
	 * @throws BadRequestException
	 *             Throw this exception if user sends a bad request
	 * @throws NotFoundException
	 *             Throw this exception if expenses not found
	 * @throws ValidationFailedException
	 *             Throw this exception if any validation fails
	 */
	@ApiOperation(value = "Gets expense reports for finance", notes = "The creation date and updated date are automatically filled.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 404, message = "Not Found"),
			@ApiResponse(code = 400, message = "Bad request, If user sends invalid data"),
			@ApiResponse(code = 404, message = "If entity does not exist") })
	@RequestMapping(value = "/getExpenseReportsForFinance", method = RequestMethod.GET)
	public @ResponseBody List<ExpenseReportEntity> getReportsForFinance(@RequestParam String status)
			throws BadRequestException, NotFoundException, ValidationFailedException {
		// call the business layer
		return expenseService.getExpenseReportsForFinance(status);
	}

}
