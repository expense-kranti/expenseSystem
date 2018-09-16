package com.boilerplate.service.implemetations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.boilerplate.database.interfaces.IExpense;
import com.boilerplate.database.interfaces.IUser;
import com.boilerplate.exceptions.rest.BadRequestException;
import com.boilerplate.exceptions.rest.NotFoundException;
import com.boilerplate.exceptions.rest.UnauthorizedException;
import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.boilerplate.framework.RequestThreadLocal;
import com.boilerplate.java.collections.BoilerplateSet;
import com.boilerplate.java.entities.ExpenseEntity;
import com.boilerplate.java.entities.ExpenseHistoryEntity;
import com.boilerplate.java.entities.ExpenseReportEntity;
import com.boilerplate.java.entities.ExpenseReviewEntity;
import com.boilerplate.java.entities.ExpenseStatusType;
import com.boilerplate.java.entities.ExternalFacingUser;
import com.boilerplate.java.entities.FetchExpenseEntity;
import com.boilerplate.java.entities.FileMappingEntity;
import com.boilerplate.java.entities.UserRoleEntity;
import com.boilerplate.java.entities.UserRoleType;
import com.boilerplate.service.interfaces.IEmailService;
import com.boilerplate.service.interfaces.IExpenseService;
import com.boilerplate.service.interfaces.IFileService;

/**
 * This class implements IExpenseService
 * 
 * @author ruchi
 *
 */
public class ExpenseService implements IExpenseService {

	/**
	 * This is the instance of IUser
	 */
	@Autowired
	IUser mySqlUser;

	/**
	 * This method set the mysqluser
	 * 
	 * @param mySqlUser
	 *            the mySqlUser to set
	 */
	public void setMySqlUser(IUser mySqlUser) {
		this.mySqlUser = mySqlUser;
	}

	/**
	 * This is the autowired instance of IExpense
	 */
	@Autowired
	IExpense mySqlExpense;

	/**
	 * This method is used to set autowired instance of IExpense
	 * 
	 * @param mySqlExpense
	 */
	public void setMySqlExpense(IExpense mySqlExpense) {
		this.mySqlExpense = mySqlExpense;
	}

	/**
	 * This is the autowired instance of IFileService
	 */
	@Autowired
	IFileService fileService;

	/**
	 * This method is used to set fileService
	 * 
	 * @param fileService
	 */
	public void setFileService(IFileService fileService) {
		this.fileService = fileService;
	}

	/**
	 * This is the autowired instance of IEmailService
	 */
	@Autowired
	IEmailService sendEmailService;

	/**
	 * This method is used to set autowired instance of IEmailService
	 * 
	 * @param sendEmailService
	 */
	public void setSendEmailService(IEmailService sendEmailService) {
		this.sendEmailService = sendEmailService;
	}

	/**
	 * @see IExpenseService.createExpense
	 */
	@Override
	public ExpenseEntity createExpense(ExpenseEntity expenseEntity) throws Exception {
		// check if expense entity is valid or not
		expenseEntity.validate();
		// entity should not have any id
		if (expenseEntity.getId() != null)
			throw new ValidationFailedException("ExpenseEntity", "Id should be null", null);
		// Check whether user id exists and is active
		ExternalFacingUser externalFacingUser = mySqlUser
				.getUser(RequestThreadLocal.getSession().getExternalFacingUser().getId());
		// set status of expense as submitted
		expenseEntity.setStatus(ExpenseStatusType.Submitted);
		expenseEntity.setUserId(externalFacingUser.getId());
		// set creation date and update date
		expenseEntity.setCreationDate(new Date());
		expenseEntity.setUpdationDate(new Date());
		// save expense in database
		expenseEntity = mySqlExpense.createExpense(expenseEntity);
		// save attachment mapping
		if (!fileService.saveFileMapping(expenseEntity)) {
			// if file mappings were not saved, delete the expense
			mySqlExpense.deleteExpense(expenseEntity);
			throw new BadRequestException("ExpenseEntity", "Could not save file mapping, please upload again", null);
		}
		// send email for submission of expense
		sendEmailService.sendEmailOnSubmission(expenseEntity, false);
		return expenseEntity;
	}

	/**
	 * @see IExpenseService.updateExpense
	 */
	@Override
	public ExpenseEntity updateExpense(ExpenseEntity expenseEntity) throws Exception {
		// check if expense entity is valid or not
		expenseEntity.validate();
		// entity should not have any id
		if (expenseEntity.getId() == null)
			throw new ValidationFailedException("ExpenseEntity", "Id should not be null", null);
		// check whether expense entity exists or not
		ExpenseEntity previousExpense = mySqlExpense.getExpense(expenseEntity.getId());
		if (previousExpense == null)
			throw new NotFoundException("ExpenseEntity", "Expense entity not found", null);

		// Check whether expense belongs to the logged in user
		if (!previousExpense.getUserId().equals(RequestThreadLocal.getSession().getExternalFacingUser().getId()))
			throw new BadRequestException("ExpenseEntity",
					"User cannot update this expense since he is not the owner of this expense", null);
		// create a new expense history entity using the data from expense
		// entity
		ExpenseHistoryEntity expenseHistoryEntity = new ExpenseHistoryEntity(previousExpense.getId(),
				previousExpense.getCreationDate(), previousExpense.getUpdationDate(), previousExpense.getTitle(),
				previousExpense.getDescription(), previousExpense.getStatus(), previousExpense.getUserId(),
				previousExpense.getApproverComments(), previousExpense.getAmount());
		expenseHistoryEntity.setCreationDate(new Date());

		// if expense status is rejected then change it to re-submitted
		if (expenseEntity.getStatus() != null) {
			if (expenseEntity.getStatus() == ExpenseStatusType.Approver_Rejected
					|| expenseEntity.getStatus() == ExpenseStatusType.Finance_Rejected) {
				expenseEntity.setStatus(ExpenseStatusType.Re_Submitted);
				// send email for re-submission of expense
				sendEmailService.sendEmailOnSubmission(expenseEntity, true);
			}
		} else
			throw new BadRequestException("ExpenseEntity", "Status should not be null", null);
		// set creation date and update date
		expenseEntity.setCreationDate(previousExpense.getCreationDate());
		expenseEntity.setUpdationDate(new Date());
		// update expense
		expenseEntity = mySqlExpense.updateExpense(expenseEntity);
		// update attachments
		List<FileMappingEntity> mappings = fileService.updateFileMapping(expenseEntity, expenseHistoryEntity);
		expenseEntity.setFileMappings(mappings);
		// save this history in mysql
		expenseHistoryEntity = mySqlExpense.saveExpenseHistory(expenseHistoryEntity);
		// send email notification
		return expenseEntity;
	}

	/**
	 * @see IExpenseService.getExpensesForUser
	 * 
	 */
	@Override
	public List<ExpenseEntity> getExpensesForUser(FetchExpenseEntity fetchExpenseEntity)
			throws ValidationFailedException, NotFoundException, BadRequestException {
		// validate entity
		fetchExpenseEntity.validate();
		// fetch list of expenses from database
		List<ExpenseEntity> expenses = mySqlExpense.getExpenses(fetchExpenseEntity);
		// check if expenses were present for the present user or not
		if (expenses == null || expenses.isEmpty())
			throw new NotFoundException("ExpenseEntity", "No expenses were found for the currently logged in user",
					null);
		// // list of expense ids
		// String expenseIds = "";
		// // for each expense, fetch its attachment
		// for (ExpenseEntity eachExpense : expenses) {
		// // put the expense id in a list
		// expenseIds += eachExpense.getId() + ",";
		// }
		// expenseIds = expenseIds.substring(0, expenseIds.length() - 1);
		// // Fetch attachments for the given list of expense ids
		// List<FileMappingEntity> fileMappings =
		// mySqlExpense.getFileMappingsForExpenses(expenseIds);
		// // for each expense mapping
		// for (ExpenseEntity expense : expenses) {
		// List<FileMappingEntity> mappings = new ArrayList<>();
		// // fetch file mappings for current expense
		// for (FileMappingEntity fileMapping : fileMappings) {
		// if
		// (fileMapping.getExpenseId().equals(String.valueOf(expense.getId())))
		// mappings.add(fileMapping);
		// }
		// expense.setFileMappings(mappings);
		// }
		return expenses;
	}

	/**
	 * @see IExpenseService.getExpensesForApproval
	 */
	@Override
	public List<ExpenseEntity> getExpensesForApproval()
			throws NotFoundException, ValidationFailedException, BadRequestException {
		// fetch approver id
		String approverId = RequestThreadLocal.getSession().getExternalFacingUser().getId();
		if (approverId == null)
			throw new ValidationFailedException("ExpenseEntity", "Approver id for fetching expenses is null or empty",
					null);
		List<ExpenseEntity> expenses = new ArrayList<>();
		// check if user is approver or super-approver
		List<UserRoleEntity> roles = mySqlUser.getUserRoles(approverId);
		for (UserRoleEntity userRoleEntity : roles) {
			// check if user is approver or super/approver
			if (userRoleEntity.getRole().equals(UserRoleType.Super_Approver)) {
				// get all the expenses
				expenses = mySqlExpense.getAllExpenses();
				break;
			} else if (userRoleEntity.getRole().equals(UserRoleType.Approver)) {
				expenses = mySqlExpense.getExpensesForApprover(approverId);
				break;
			}
		}

		// check if expenses are not null
		if (expenses.size() == 0)
			throw new BadRequestException("ExpenseEntity", "No expenses found", null);
		// list of expense ids
		String expenseIds = "";
		// for each expense, fetch its attachment
		for (ExpenseEntity eachExpense : expenses) {
			// put the expense id in a list
			expenseIds += eachExpense.getId() + ",";
		}
		expenseIds = expenseIds.substring(0, expenseIds.length() - 1);
		// Fetch attachments for the given list of expense ids
		List<FileMappingEntity> fileMappings = mySqlExpense.getFileMappingsForExpenses(expenseIds);
		// create a list of expenses
		List<ExpenseEntity> expenseList = new ArrayList<>();
		// for each expense mapping
		for (ExpenseEntity expense : expenses) {
			List<FileMappingEntity> attachments = new ArrayList<>();
			// fetch file mappings for current expense
			for (FileMappingEntity fileMapping : fileMappings) {
				if (fileMapping.getExpenseId().equals(expense.getId())) {
					FileMappingEntity attachmentEntity = new FileMappingEntity(fileMapping.getAttachmentId(),
							RequestThreadLocal.getSession().getExternalFacingUser().getId(), expense.getId(), true,
							null, fileMapping.getOriginalFileName(), fileMapping.getContentType());
					attachments.add(attachmentEntity);
				}
			}
			expense.setFileMappings(attachments);
			// add the expense entity in list
			expenseList.add(expense);
		}
		return expenseList;
	}

	/**
	 * @see IExpenseService.approverExpense
	 */
	@Override
	public ExpenseEntity approveExpenseForApprover(ExpenseReviewEntity expenseReviewEntity) throws Exception {
		// check if user id or role is not null or empty
		if (expenseReviewEntity == null)
			throw new ValidationFailedException("ExpenseReviewEntity", "ExpenseId should not be null or empty", null);
		// check if expense is being rejected, comments are mandatory
		if (expenseReviewEntity.getStatus().equals(ExpenseStatusType.Approver_Rejected)
				&& expenseReviewEntity.getApproverComments() == null)
			throw new ValidationFailedException("ExpenseReviewEntity",
					"Comments are mandatory if expense is being rejected", null);
		// check if expense exists and is active
		ExpenseEntity expenseEntity = mySqlExpense.getExpense(expenseReviewEntity.getExpenseId());
		// check if expense is active
		if (expenseEntity == null)
			throw new NotFoundException("ExpenseEntity", "Expense not found", null);
		// check if expense is in submitted or re-submitted state
		if (!expenseEntity.getStatus().equals(ExpenseStatusType.Submitted)
				&& !expenseEntity.getStatus().equals(ExpenseStatusType.Re_Submitted))
			throw new BadRequestException("ExpenseEntity",
					"Expense is not in desired state for approver/super-approver to take action", null);
		// fetch user of this expense
		ExternalFacingUser externalFacingUser = mySqlUser.getUser(expenseEntity.getUserId());
		// check if user is not null or inactive
		if (externalFacingUser == null || !externalFacingUser.getIsActive())
			throw new BadRequestException("ExternalFacingUser", "User not found for the given expense or is inactive",
					null);
		String approverId = RequestThreadLocal.getSession().getExternalFacingUser().getId();
		// check if approver is super-approver or matches with approver id
		// assigned to the user
		if (!RequestThreadLocal.getSession().getExternalFacingUser().getRoles().contains(UserRoleType.Super_Approver)
				&& !approverId.equals(externalFacingUser.getApproverId()))
			throw new UnauthorizedException("ExpenseEntity", "User is not authorized to approve/reject this expense",
					null);
		// match the approver/super approver with currently logged in user
		if (!externalFacingUser.getApproverId().equals(approverId))
			throw new UnauthorizedException("ExpenseEntity",
					"User is not assigned as approver/super approver for this expense", null);
		// create a new expense history entity using the data from expense
		// entity
		ExpenseHistoryEntity expenseHistoryEntity = new ExpenseHistoryEntity(expenseEntity.getId(),
				expenseEntity.getCreationDate(), expenseEntity.getUpdationDate(), expenseEntity.getTitle(),
				expenseEntity.getDescription(), expenseEntity.getStatus(), expenseEntity.getUserId(),
				expenseEntity.getApproverComments(), expenseEntity.getAmount());
		expenseHistoryEntity.setCreationDate(new Date());
		// save this history in mysql
		expenseHistoryEntity = mySqlExpense.saveExpenseHistory(expenseHistoryEntity);
		// set expense status as per the approving role
		expenseEntity.setStatus(expenseReviewEntity.getStatus());
		// set approver comments
		expenseEntity.setApproverComments(expenseReviewEntity.getApproverComments());
		// update expense
		expenseEntity = mySqlExpense.updateExpense(expenseEntity);
		if (expenseReviewEntity.getStatus().equals(ExpenseStatusType.Approver_Approved))
			sendEmailService.sendEmailOnApproval(expenseEntity);
		else if (expenseReviewEntity.getStatus().equals(ExpenseStatusType.Approver_Rejected))
			sendEmailService.sendEmailOnRejection(expenseEntity);
		return expenseEntity;

	}

	/**
	 * @see IExpenseService.getExpensesForFinance
	 */
	@Override
	public List<ExpenseReportEntity> getExpensesForFinance() throws BadRequestException {
		// get the currently logged in user's id
		String financeId = RequestThreadLocal.getSession().getExternalFacingUser().getId();
		// get list of expenses filed under the given approver
		List<Map<String, Object>> expenseMap = mySqlExpense.getExpensesForFinance(financeId,
				ExpenseStatusType.Finance_Approved);
		// check if expenses are not null
		if (expenseMap.size() == 0)
			throw new BadRequestException("ExpenseEntity", "No expenses found", null);
		// list of expense ids
		String expenseIds = "";
		// set for expense ids
		BoilerplateSet<String> userIds = new BoilerplateSet<>();
		// for each expense, fetch its attachment
		for (Map<String, Object> eachExpense : expenseMap) {
			// put the expense id in a list
			expenseIds += eachExpense.get("id") + ",";
			// put id in set
			userIds.add(String.valueOf(eachExpense.get("userId")));
		}
		expenseIds = expenseIds.substring(0, expenseIds.length() - 1);
		// Fetch attachments for the given list of expense ids
		List<FileMappingEntity> fileMappings = mySqlExpense.getFileMappingsForExpenses(expenseIds);
		// create a list of expenses
		List<ExpenseEntity> expenseList = new ArrayList<>();
		// for each expense mapping
		for (Map<String, Object> expense : expenseMap) {
			List<FileMappingEntity> attachments = new ArrayList<>();
			// fetch file mappings for current expense
			for (FileMappingEntity fileMapping : fileMappings) {
				if (fileMapping.getExpenseId().equals(String.valueOf(expense.get("id")))) {
					FileMappingEntity attachmentEntity = new FileMappingEntity(fileMapping.getAttachmentId(),
							RequestThreadLocal.getSession().getExternalFacingUser().getId(),
							String.valueOf(expense.get("id")), true, null, fileMapping.getOriginalFileName(),
							fileMapping.getContentType());
					attachments.add(attachmentEntity);
				}
			}

			// create a new expense entity
			ExpenseEntity expenseEntity = new ExpenseEntity(String.valueOf(expense.get("id")),
					String.valueOf(expense.get("title")), String.valueOf(expense.get("description")),
					ExpenseStatusType.valueOf(String.valueOf(expense.get("status"))), attachments,
					String.valueOf(expense.get("userId")), String.valueOf(expense.get("name")),
					String.valueOf(expense.get("approverComments")),
					Float.valueOf(String.valueOf(expense.get("amount"))), (Date) expense.get("creationDate"),
					(Date) expense.get("updatedDate"));
			// add the expense entity in list
			expenseList.add(expenseEntity);
		}
		// List of expense reports
		List<ExpenseReportEntity> reportEntities = new ArrayList<>();
		// for each expense in unique set
		for (String userId : userIds) {
			// List of expenses
			List<ExpenseEntity> expenses = new ArrayList<>();
			// total amount
			float totalAmount = 0f;
			// name of the user
			String name = null;
			// traverse through list of expense
			for (ExpenseEntity expenseEntity : expenseList) {
				// if expense belongs to user
				if (expenseEntity.getUserId().equals(userId)) {
					// add the expense to the list of expenses
					expenses.add(expenseEntity);
					// add the amount in total amount
					totalAmount += expenseEntity.getAmount();
					// set name
					name = expenseEntity.getUserName();
				}
			}
			// create a new expense report entity
			ExpenseReportEntity reportEntity = new ExpenseReportEntity(name, userId, totalAmount, expenses,
					ExpenseStatusType.Finance_Approved);
			// add the report entity in list
			reportEntities.add(reportEntity);
		}
		return reportEntities;

	}

	/**
	 * @see IExpenseService.approveExpenseForFinance
	 */
	@Override
	public void approveExpenseForFinance(ExpenseReportEntity reportEntity) throws Exception {
		// validate the report entity
		// check if user in report exists
		ExternalFacingUser user = mySqlUser.getUser(reportEntity.getUserId());
		if (user == null || !user.getIsActive())
			throw new ValidationFailedException("ExternalFacingUser",
					"User id in report doesn not exist or the user is inactive", null);
		// for each expense in report entity
		for (ExpenseEntity expense : reportEntity.getExpenses()) {
			// check if user in expense is same as report
			if (!expense.getUserId().equals(reportEntity.getUserId()))
				throw new ValidationFailedException("ExpenseReportEntity",
						"One of the expense in expense list of the report does not belong to the user mentioned in report",
						null);
			// check if status of each expense is approver approved
			if (!expense.getStatus().equals(ExpenseStatusType.Approver_Approved))
				if (!expense.getStatus().equals(ExpenseStatusType.Finance_Approved))
					throw new ValidationFailedException("ExpenseReportEntity",
							"One of the expense in expense list of the report is not in desired status", null);
			// set status
			expense.setStatus(reportEntity.getStatus());
		}
		// update all expenses
		mySqlExpense.saveExpenseList(reportEntity.getExpenses());

	}

	/**
	 * @see IExpenseService.expenseReviewByFinance
	 */
	@Override
	public void expenseReviewByFinance(ExpenseReviewEntity expenseReviewEntity)
			throws ValidationFailedException, BadRequestException, NotFoundException {
		// validate the expenseReviewEntity
		expenseReviewEntity.validate();
		// check if status is rejected than approver comments should be present
		if (expenseReviewEntity.getStatus() == ExpenseStatusType.Finance_Rejected)
			if (expenseReviewEntity.getApproverComments().equals(null))
				throw new ValidationFailedException("ExpenseReviewEntity",
						"Approver comments are mandatory if expense is being rejected", null);
		// check if expense is present in database or not
		ExpenseEntity expenseEntity = mySqlExpense.getExpense(expenseReviewEntity.getExpenseId());
		if (expenseEntity == null)
			throw new NotFoundException("ExpenseEntity", "Expense not found with given expense id", null);
		// check if expense is in approver approved state
		if (!expenseEntity.getStatus().equals(ExpenseStatusType.Approver_Approved))
			throw new BadRequestException("ExpenseEntity", "Expense is not in desired state for finance to take action",
					null);
		// update the expense entity
		expenseEntity.setStatus(expenseReviewEntity.getStatus());
		// update the expense entity in the system
		mySqlExpense.updateExpense(expenseEntity);
	}

}
