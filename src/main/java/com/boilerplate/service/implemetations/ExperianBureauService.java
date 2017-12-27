package com.boilerplate.service.implemetations;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.boilerplate.asyncWork.ParseExperianReportObserver;
import com.boilerplate.database.interfaces.IExperian;
import com.boilerplate.exceptions.rest.BadRequestException;
import com.boilerplate.exceptions.rest.ConflictException;
import com.boilerplate.exceptions.rest.NotFoundException;
import com.boilerplate.exceptions.rest.PreconditionFailedException;
import com.boilerplate.exceptions.rest.UnauthorizedException;
import com.boilerplate.exceptions.rest.UpdateFailedException;
import com.boilerplate.exceptions.rest.ValidationFailedException;
import com.boilerplate.framework.HttpResponse;
import com.boilerplate.framework.HttpUtility;
import com.boilerplate.framework.Logger;
import com.boilerplate.framework.RequestThreadLocal;
import com.boilerplate.java.Base;
import com.boilerplate.java.collections.BoilerplateList;
import com.boilerplate.java.collections.BoilerplateMap;
import com.boilerplate.java.entities.ExperianQuestionAnswer;
import com.boilerplate.java.entities.ExternalFacingReturnedUser;
import com.boilerplate.java.entities.FileEntity;
import com.boilerplate.java.entities.HttpEntity;
import com.boilerplate.java.entities.MethodState;
import com.boilerplate.java.entities.Report;
import com.boilerplate.java.entities.ReportInputEntiity;
import com.boilerplate.java.entities.ReportSource;
import com.boilerplate.java.entities.ReportStatus;
import com.boilerplate.java.entities.ReportVersion;
import com.boilerplate.java.entities.Voucher;
import com.boilerplate.java.entities.ExperianDataPublishEntity.State;
import com.boilerplate.service.interfaces.IExperianService;
import com.boilerplate.service.interfaces.IFileService;
import com.boilerplate.service.interfaces.IReportService;
import com.boilerplate.service.interfaces.IUserService;

/**
 * 
 * @author love
 *
 */
public class ExperianBureauService implements IExperianService {
	/**
	 * This is an instance of the logger
	 */
	Logger logger = Logger.getInstance(ExperianBureauService.class);

	/**
	 * This is the instance of fileservice
	 */
	@Autowired
	private IFileService fileService;

	/**
	 * This method set the file service
	 * 
	 * @param fileService
	 */
	public void setFileService(IFileService fileService) {
		this.fileService = fileService;
	}

	/**
	 * This is the instance of report service
	 */
	@Autowired
	private IReportService reportService;

	/**
	 * This method set the report service
	 * 
	 * @param reportService
	 */
	public void setReportService(IReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * This is an instance of the queue job, to save the session back on to the
	 * database async
	 */
	@Autowired
	com.boilerplate.jobs.QueueReaderJob queueReaderJob;

	/**
	 * This sets the queue reader jon
	 * 
	 * @param queueReaderJob
	 *            The queue reader jon
	 */
	public void setQueueReaderJob(
			com.boilerplate.jobs.QueueReaderJob queueReaderJob) {
		this.queueReaderJob = queueReaderJob;
	}

	/**
	 * The RedisSFUpdateHash
	 */
	@Autowired
	com.boilerplate.database.redis.implementation.RedisSFUpdateHash redisSFUpdateHashAccess;

	/**
	 * This method sets RedisSFUpdateHash
	 * 
	 * @return the redis sf update hash
	 */
	public void setRedisSFUpdateHashAccess(
			com.boilerplate.database.redis.implementation.RedisSFUpdateHash redisSFUpdateHashAccess) {
		this.redisSFUpdateHashAccess = redisSFUpdateHashAccess;
	}

	/**
	 * This is the user service
	 */
	@Autowired
	IUserService userService;

	/**
	 * sets the user service
	 * 
	 * @param userService
	 *            The user service
	 */
	public void setUserService(IUserService userService) {
		this.userService = userService;
	}

	/**
	 * This is the instance of the configuration manager.
	 */
	@Autowired
	com.boilerplate.configurations.ConfigurationManager configurationManager;

	/**
	 * The setter to set the configuration manager
	 * 
	 * @param configurationManager
	 */
	public void setConfigurationManager(
			com.boilerplate.configurations.ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}

	/**
	 * This is the instance of experian data access.
	 */
	@Autowired
	IExperian experianDataAccess;

	/**
	 * This method set the experian data access layer
	 * 
	 * @param experianDataAccess
	 */
	public void setExperianDataAccess(IExperian experianDataAccess) {
		this.experianDataAccess = experianDataAccess;
	}

	/**
	 * The subject list for experian report
	 */
	BoilerplateList<String> subjectsForParseExperianReportObserver = new BoilerplateList();
	BoilerplateList<String> subjectsForExperianKYCUpload = new BoilerplateList();

	/**
	 * Initializes the bean
	 * 
	 * @throws JAXBException
	 */
	public void initilize() throws JAXBException {
		subjectsForParseExperianReportObserver.add("ParseExperianReport");
		subjectsForExperianKYCUpload.add("SendSMSOnReportFailAfterPayment");
	}

	/**
	 * This is the instance of parse report
	 */
	@Autowired
	private ParseExperianReportObserver parseExperianReportObserver;

	/**
	 * This method set the parse experian report observer.
	 * 
	 * @param parseExperianReportObserver
	 */
	public void setParseExperianReportObserver(
			ParseExperianReportObserver parseExperianReportObserver) {
		this.parseExperianReportObserver = parseExperianReportObserver;
	}

	/**
	 * @see IExperianService.startSingle
	 */
	@Override
	public ReportInputEntiity startSingle(ReportInputEntiity reportInputEntiity)
			throws ConflictException, UnauthorizedException,
			ValidationFailedException, NotFoundException, BadRequestException,
			IOException, PreconditionFailedException {
		// check if pan exist
		this.panNumberValidation(reportInputEntiity);

		reportInputEntiity
				.setExperianAttemptDate(new java.util.Date().toString());
		reportInputEntiity.setUserId(RequestThreadLocal.getSession()
				.getExternalFacingUser().getId());
		reportInputEntiity.setUserLoginId(RequestThreadLocal.getSession()
				.getExternalFacingUser().getUserId());
		reportInputEntiity.validate();

		// Save this information into the current users metadata for future
		// reference
		ExternalFacingReturnedUser user = userService.get(RequestThreadLocal
				.getSession().getExternalFacingUser().getUserId(), false);
		user.setExperianRequestUniqueKey(reportInputEntiity.getMobileNumber());
		user.setReportInputEntitty(reportInputEntiity);
		userService.update(user);

		// now get the voucher code for this user
		if (reportInputEntiity.getVoucherCode() == null
				|| reportInputEntiity.getVoucherCode().equals("")) {
			Voucher voucher = experianDataAccess.getVoucherCode(user.getId(),
					RequestThreadLocal.getSession().getSessionId());
			reportInputEntiity.setVoucherCode(voucher.getVoucherCode());
			reportInputEntiity.setVoucherExpiry(voucher.getExpiryDate());
		}
		// Save this into the session for easy access
		RequestThreadLocal.getSession().addSessionAttribute("ExperianData",
				reportInputEntiity);
		reportInputEntiity = this
				.doStartSingleURLIntegration(reportInputEntiity, user);

		user.setReportInputEntitty(reportInputEntiity);
		userService.update(user);
		logger.logInfo("ExperianBureauService", "Single Request API",
				"Complete processing" + reportInputEntiity.getUserId(), "");
		return reportInputEntiity;
	}

	/**
	 * This method throws ConflictException if report with this pan already
	 * exists or not
	 * 
	 * @param reportInputEntiity
	 *            The reportInputEntiity
	 * @throws ConflictException
	 *             The ConflictException
	 */
	private void panNumberValidation(ReportInputEntiity reportInputEntiity)
			throws ConflictException {
		if (reportInputEntiity.getPanNumber() != null) {
			if (this.redisSFUpdateHashAccess.hget(
					configurationManager.get("PanNumberHash_Base_Tag"),
					reportInputEntiity.getPanNumber().toUpperCase()) != null) {
				throw new ConflictException("Report",
						"Report with given pan number already exists", null);
			}
		}

	}

	private ReportInputEntiity doStartSingleURLIntegration(
			ReportInputEntiity reportInputEntiity,
			ExternalFacingReturnedUser user)
			throws IOException, PreconditionFailedException, NotFoundException,
			ConflictException, BadRequestException {
		reportInputEntiity.setStateEnum(ReportInputEntiity.State.SessionSetup);
		String requestData = createExperianSingleURLRequestBody(
				reportInputEntiity);

		BoilerplateMap<String, BoilerplateList<String>> requestHeaders = createExperianSingleUrlRequestHeaders();

		String request = createExperianRequest(requestHeaders, requestData);

		BoilerplateMap<String, BoilerplateList<String>> requestHeadersJava = new BoilerplateMap();
		BoilerplateList<String> headerValueJava = new BoilerplateList<String>();
		BoilerplateList<String> contentTypeHeaderValueJava = new BoilerplateList<String>();
		contentTypeHeaderValueJava.add("application/json;charset=UTF-8");
		requestHeadersJava.put("Content-Type", contentTypeHeaderValueJava);

		logger.logInfo("ExperianBureauService", "Single Request","Request" + reportInputEntiity.getUserId(), request);
		HttpResponse httpResponseJava = HttpUtility.makeHttpRequest(
				configurationManager.get("Experian_INITIATE_URL_JAVA"),
				requestHeadersJava, null, request, "POST");
		logger.logInfo("ExperianBureauService", "Single Response","Response Status code" + reportInputEntiity.getUserId(),Integer.toString(httpResponseJava.getHttpStatus()));
		logger.logInfo("ExperianBureauService", "Single Response","Response" + reportInputEntiity.getUserId(),httpResponseJava.getResponseBody());

		HttpResponse httpResponse = Base.fromJSON(
				httpResponseJava.getResponseBody(), HttpResponse.class);

		String ecvSessionValue = null;
		if (httpResponse.getResponseCookies() != null) {

			for (HttpCookie cookie : httpResponse.getResponseCookies()) {
				if (cookie.getName().equals("JSESSIONID")) {
					logger.logInfo("ExperianBureauService","Single Response Cookie ecv","Response" + reportInputEntiity.getUserId(),cookie.getValue());
					ecvSessionValue = cookie.getValue();
				}
			}
		}
		if (httpResponse.getHttpStatus() != 200) {
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntiity,
					"--- Method Name: " + " Single Action --- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: Issue in accessing server - Experian_Single_Request ",
					true);
			throw new PreconditionFailedException("Experian Server","Issue in accessing server - Experian_Single_Request",null);
		}
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> responseBodyMap = objectMapper
					.readValue(httpResponse.getResponseBody(), Map.class);

			// check the case where report comes up
			if (responseBodyMap.get("errorString") == null && responseBodyMap
					.get("showHtmlReportForCreditReport") != null) {
				// we have a report
				// save this to the users context
				// and change state to next question

				reportInputEntiity.setStateEnum(State.Report);
				String report = (String) responseBodyMap
						.get("showHtmlReportForCreditReport");
				MockMultipartFile file = new MockMultipartFile("experianreport.html", "experianreport.html","text/html", report.getBytes());
				FileEntity fileEntity = fileService.saveFile("ExperianReport",
						file);
				reportInputEntiity.setReportFileEntity(fileEntity);
				// saves the experian report url in the corresponding user

				// Open the file
				String htmlFile = FileUtils.readFileToString(new File(
						configurationManager.get("RootFileDownloadLocation")
								+ fileEntity.getFileName()));

				// cut out the xml part from it
				int startingPOsition = htmlFile.indexOf("xmlResponse") + 21;
				String xmlFile = htmlFile.substring(startingPOsition,
						htmlFile.length());
				String reportNumber = getReportNumber(xmlFile);
				reportInputEntiity.setReportNumber(reportNumber);

				user = observeReport(reportInputEntiity, user);
				user.setExperianReportUrl(fileEntity.getFullFileNameOnDisk());
				user.setUserState(MethodState.ReportGenerated);
				// updates the experianStatus:(15-11-2016)
				experianStatusUpdate(reportInputEntiity, "--- Method Name: fetchNextItem " + "--- Response status: "+ httpResponse.getHttpStatus()+"--- Error String and Response Json: "
						+ getErrorStringAndResponseJsonFields(responseBodyMap),
						false);

				user.setReportInputEntitty(reportInputEntiity);
				userService.update(user);
				// publishUserStateToCRM(user);
				RequestThreadLocal.getSession().addSessionAttribute(
						"ExperianData", reportInputEntiity);
				return reportInputEntiity;

			}

			if (responseBodyMap.get("errorString") != null) {
				logger.logInfo("ExperianBureauService","Single Response errorstring",responseBodyMap.get("errorString").toString(), "");
				if (responseBodyMap.get("errorString").toString()
						.contains("consumer record not found") == true) {
					// updates the experianStatus:(15-11-2016)
					experianStatusUpdate(reportInputEntiity,
							"--- Method Name: " + " singleAction --- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: User not found in Bureau database - Dual Integration --- Error String and Response Json: "
									+ getErrorStringAndResponseJsonFields(
											responseBodyMap),
							true);
					// sets the pan number in pan number hash
					setPanNumberInHash(reportInputEntiity);
					throw new NotFoundException("Experian Server",
							"User not found in Bureau database - Dual Integration No record found. Please re-submit your request. ",
							null);
				}
				if (responseBodyMap.get("errorString").toString()
						.contains("Voucher Code is invalid") == true) {
					// updates the experianStatus:(15-11-2016)
					experianStatusUpdate(reportInputEntiity,
							"--- Method Name: " + " singleAction --- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: User not found in Bureau database - Dual Integration --- Error String and Response Json: "
									+ getErrorStringAndResponseJsonFields(
											responseBodyMap),
							true);
					throw new PreconditionFailedException("Experian Server",
							"Voucher code invalid - Dual Integration Voucher code invalid. Please re-submit your request. ",
							null);
				}
				

			}

			if (responseBodyMap.get("stageOneId_") != null
					&& responseBodyMap.get("stageTwoId_") != null
					&& ecvSessionValue != null) {
				// set parameters in reportinputentity
				reportInputEntiity.setStage1Id(
						responseBodyMap.get("stageOneId_").toString());
				reportInputEntiity.setStage2Id(
						responseBodyMap.get("stageTwoId_").toString());
				reportInputEntiity.setjSessionId2(ecvSessionValue);
				reportInputEntiity
						.setStateEnum(ReportInputEntiity.State.SessionSetup);
				logger.logInfo("ExperianBureauService", "Single Action","set response parameters"+ reportInputEntiity.getUserId(),"");
			} // in case of any error
			else {
				// updates the experianStatus:(15-11-2016)
				experianStatusUpdate(reportInputEntiity, "--- Method Name: singleAction " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: Stageone id or stage2 id or cookie not returned - singleAction data not returned --- Error String and Response Json: "
						+ getErrorStringAndResponseJsonFields(responseBodyMap),
						true);
				throw new PreconditionFailedException("Experian Server","Data not returned - singleAction url not returned",null);
			}

		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (PreconditionFailedException pfe) {
			throw pfe;
		} catch (Throwable th) {
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntiity, "--- Method Name: singleAction " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: Data  not returned - singleAction_Form_Action "+ th.toString() + "--- Error String and Response Json: "+ getErrorStringAndResponseJsonFields(httpResponse), true);
			throw new PreconditionFailedException("Experian Server",
					"Data not returned - singleAction " + th.toString(), null);
		}
		return reportInputEntiity;
	}

	/**
	 * This method create the request for sending the data to experian server.
	 * 
	 * @param requestHeaders
	 *            The request headers which is use for sending the experian
	 *            request.
	 * @param requestBody
	 *            The request body
	 * @return The request body
	 */
	private String createExperianRequest(
			BoilerplateMap<String, BoilerplateList<String>> requestHeaders,
			String requestBody) {
		String request = configurationManager.get("JAVA_HTTP_REQUEST_TEMPLATE");
		request = request.replace("@url",
				configurationManager.get("Experian_Single_Request_URL"));
		request = request.replace("@requestHeaders", requestHeaders.toString());
		request = request.replace("@requestBody", requestBody);
		request = request.replace("@method", "POST");
		return request;
	}

	/**
	 * This method create the request headers for experian request
	 * 
	 * @return The required request headers
	 */
	private BoilerplateMap<String, BoilerplateList<String>> createExperianSingleUrlRequestHeaders() {
		BoilerplateMap<String, BoilerplateList<String>> requestHeaders = new BoilerplateMap();
		BoilerplateList<String> headerValue = new BoilerplateList<String>();
		headerValue.add(configurationManager.get("Experian_User_Agent"));
		requestHeaders.put("User-Agent", headerValue);

		BoilerplateList<String> contentTypeHeaderValue = new BoilerplateList<String>();
		contentTypeHeaderValue.add("application/x-www-form-urlencoded");
		requestHeaders.put("Content-Type", contentTypeHeaderValue);
		return requestHeaders;
	}

	/**
	 * This method create the request body for experian request.
	 * 
	 * @param reportInputEntiity
	 *            The report input entity
	 * @return The required request body
	 */
	private String createExperianSingleURLRequestBody(
			ReportInputEntiity reportInputEntiity) {
		String requestBody = configurationManager
				.get("Experian_Single_Url_Request_Template");
		requestBody = requestBody.replace("{voucherCode}",
				reportInputEntiity.getVoucherCode());
		requestBody = requestBody.replace("{firstName}",
				reportInputEntiity.getFirstName());
		requestBody = requestBody.replace("{surName}",
				reportInputEntiity.getSurname());
		requestBody = requestBody.replace("{dob}",
				reportInputEntiity.getDateOfBirth());
		requestBody = requestBody.replace("{gender}",
				Integer.toString(reportInputEntiity.getGenderEnum().ordinal()));
		requestBody = requestBody.replace("{mobileNo}",
				reportInputEntiity.getMobileNumber());
		requestBody = requestBody.replace("{email}",
				reportInputEntiity.getEmail().replace("@", "%40"));
		String flatPlotHouseNo = reportInputEntiity.getAddressLine1();
		if (reportInputEntiity.getAddressLine2() != null) {
			if (reportInputEntiity.getAddressLine2().equals("") == false) {
				flatPlotHouseNo += " " + reportInputEntiity.getAddressLine2();
			}
		}
		requestBody = requestBody.replace("{flatno}",
				reportInputEntiity.getAddressLine1().replace(" ", "+"));
		requestBody = requestBody.replace("{road}",
				reportInputEntiity.getAddressLine2() == null ? ""
						: reportInputEntiity.getAddressLine2().replace(" ",
								"+"));
		requestBody = requestBody.replace("{city}",
				reportInputEntiity.getCity());
		requestBody = requestBody.replace("{state}",
				reportInputEntiity.getStateId());
		requestBody = requestBody.replace("{pincode}",
				reportInputEntiity.getPinCode());
		requestBody = requestBody.replace("{panNo}",
				reportInputEntiity.getPanNumber() == null ? ""
						: reportInputEntiity.getPanNumber());
		requestBody = requestBody.replace("{middleName}",
				reportInputEntiity.getMiddleName() == null ? ""
						: reportInputEntiity.getMiddleName());
		requestBody = requestBody.replace("{telePhoneNo}",
				reportInputEntiity.getTelephoneNumber() == null ? ""
						: reportInputEntiity.getTelephoneNumber());
		requestBody = requestBody.replace("{telePhoneType}",
				reportInputEntiity.getTelephoneTypeId() == null ? ""
						: reportInputEntiity.getTelephoneTypeId());
		requestBody = requestBody.replace("{passportNo}",
				reportInputEntiity.getPassportNumber() == null ? ""
						: reportInputEntiity.getPassportNumber());
		requestBody = requestBody.replace("{voterIdNo}",
				reportInputEntiity.getVoterIdNumber() == null ? ""
						: reportInputEntiity.getVoterIdNumber());
		requestBody = requestBody.replace("{universalIdNo}",
				reportInputEntiity.getUniversalIdNumber() == null ? ""
						: reportInputEntiity.getUniversalIdNumber());
		requestBody = requestBody.replace("{driverLicenseNo}",
				reportInputEntiity.getDriverLicenseNumber() == null ? ""
						: reportInputEntiity.getDriverLicenseNumber());
		return requestBody;

	}

	/**
	 * This method finds the value of node on the basis of tag name.
	 * 
	 * @param tagName
	 *            node name
	 * @param nodes
	 *            nodes list
	 * @return node value
	 */
	private Node getNode(String tagName, NodeList nodes) {

		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeName().equalsIgnoreCase(tagName)) {
				return node;
			}
		}

		return null;
	}

	/**
	 * This method converts a string from experian into a double
	 * 
	 * @param str
	 *            The String
	 * @param defaulValue
	 *            The default value
	 * @return double value
	 */
	private String getNodeValue(String tagName, NodeList nodes) {
		for (int j = 0; j < nodes.getLength(); j++) {
			Node node = nodes.item(j);
			if (node.getNodeName().equalsIgnoreCase(tagName)) {
				NodeList childNodes = node.getChildNodes();
				for (int k = 0; k < childNodes.getLength(); k++) {
					Node data = childNodes.item(k);
					if (data.getNodeType() == Node.TEXT_NODE)
						return data.getNodeValue();
				}
			}
		}
		return "";
	}

	/**
	 * This method sets the pan number inside the pan number list hash
	 * 
	 * @param reportInputEntiity
	 *            The reportInputEntiity
	 * @throws NotFoundException
	 *             The NotFoundException
	 * @throws BadRequestException
	 *             The BadRequestException
	 */
	private void setPanNumberInHash(ReportInputEntiity reportInputEntiity)
			throws NotFoundException, BadRequestException {
		if (reportInputEntiity.getPanNumber() != null) {
			this.redisSFUpdateHashAccess.hset(
					configurationManager.get("PanNumberHash_Base_Tag"),
					reportInputEntiity.getPanNumber().toUpperCase(),
					reportInputEntiity.getUserId());
		}

	}

	/**
	 * This method will re inserts the unused vouchers in queue again.
	 * 
	 * @param reportInputEntity
	 *            The reportInputEntity
	 */
	private void reInsertUnusedVouchers(ReportInputEntiity reportInputEntity) {
		logger.logInfo("ExperianBureauService", "reInsertUnusedVouchers",
				"Starts creating voucher List", "");
		Voucher voucher = new Voucher();
		voucher.setVoucherCode(reportInputEntity.getVoucherCode());
		voucher.setExpiryDate(reportInputEntity.getVoucherExpiry());
		BoilerplateList<Voucher> vouchersList = new BoilerplateList<>();
		vouchersList.add(voucher);
		experianDataAccess.create(vouchersList);
		logger.logInfo("ExperianBureauService", "reInsertUnusedVouchers",
				"Susccessfully inserted voucher List in Voucher queue",
				"Voucher:" + Base.toJSON(vouchersList));
	}

	/**
	 * This method re insert voucher if it is unused, updates the experian
	 * attempt status and also publish to CRM
	 * 
	 * @param reportInputEntity
	 * @throws ConflictException
	 * @throws BadRequestException
	 * @throws NotFoundException
	 */
	private void experianStatusUpdate(ReportInputEntiity reportInputEntity,
			String experianStatus, boolean isVoucherUnUsed)
			throws ConflictException, NotFoundException, BadRequestException {
		if (isVoucherUnUsed) {
			reInsertUnusedVouchers(reportInputEntity);
		}
		reportInputEntity.setExperianStatus(experianStatus);
		// Save this information into the current users metadata for future
		// reference
		ExternalFacingReturnedUser user = userService.get(RequestThreadLocal
				.getSession().getExternalFacingUser().getUserId(), false);
		user.getUserMetaData().put("ExperianData", reportInputEntity.toJSON());
		user.setReportInputEntitty(reportInputEntity);
		userService.update(user);
		if (reportInputEntity.getStage1Id() != null
				&& reportInputEntity.getStage1Id() != "") {
			// publishUserStateToCRM(user);
		}

	}

	/**
	 * This is the overloaded method of method
	 * getErrorStringAndResponseJsonFields which gets the error string from http
	 * response
	 * 
	 * @param httpResponse
	 *            The httpResponse
	 * @return The error string
	 */
	private String getErrorStringAndResponseJsonFields(
			HttpResponse httpResponse) {
		String errorAndresponseJsonString = "";
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> responseBodyMap = objectMapper
					.readValue(httpResponse.getResponseBody(), Map.class);
			if (responseBodyMap.get("errorString") != null && responseBodyMap
					.get("errorString").toString().equals("") == false) {
				errorAndresponseJsonString = responseBodyMap.get("errorString")
						.toString();
			}
			if (responseBodyMap.get("responseJson") != null && responseBodyMap
					.get("responseJson").toString().equals("") == false) {
				errorAndresponseJsonString += responseBodyMap
						.get("responseJson").toString();
			}
		} catch (Exception e) {
			logger.logInfo("ExperianBureauService",
					"getErrorStringAndResponseJsonFields", "",
					"while getting error string from http response:"
							+ e.toString());
		}
		return errorAndresponseJsonString;
	}

	/**
	 * This method gets the error string from http response
	 * 
	 * @param httpResponse
	 *            The httpResponse
	 * @return The error string
	 */
	private String getErrorStringAndResponseJsonFields(
			Map<String, Object> responseBodyMap) {
		String errorAndresponseJsonString = "";
		try {
			if (responseBodyMap.get("errorString") != null && responseBodyMap
					.get("errorString").toString().equals("") == false) {
				errorAndresponseJsonString = errorAndresponseJsonString
						+ " errorString- "
						+ responseBodyMap.get("errorString").toString();
			}
			if (responseBodyMap.get("responseJson") != null && responseBodyMap
					.get("responseJson").toString().equals("") == false) {
				errorAndresponseJsonString = errorAndresponseJsonString
						+ " responseJson- "
						+ responseBodyMap.get("responseJson").toString();
			}
			if (responseBodyMap.get("errorMessage") != null && responseBodyMap
					.get("errorMessage").toString().equals("") == false) {
				errorAndresponseJsonString = errorAndresponseJsonString
						+ " errorMessage- "
						+ responseBodyMap.get("errorMessage").toString();
			}
		} catch (Exception e) {
			logger.logInfo("ExperianBureauService", "isErrorStringExists", "",
					"while getting error string from http response:"
							+ e.toString());
		}
		return errorAndresponseJsonString;
	}

	private ExternalFacingReturnedUser observeReport(
			ReportInputEntiity reportInputEntity,
			ExternalFacingReturnedUser user)
			throws IOException, ConflictException {
		try {
			// make an entry for the report
			Report report = new Report();
			// set report version
			report = checkOrSetReportVersion(reportInputEntity, report);
			report.setFileId(reportInputEntity.getReportFileEntity().getId());
			report.setUserId(reportInputEntity.getUserId());
			report.setReportSourceEnum(ReportSource.Experian);
			report.setReportStatusEnum(ReportStatus.InProgress);
			report.setQuestionCount(reportInputEntity.getQuestionCount());
			report.setReportNumber(reportInputEntity.getReportNumber());
			reportService.save(report);
			reportInputEntity.setReport(report);
			user = userService.get(reportInputEntity.getUserLoginId(), false);
			user.setUserState(MethodState.Report);
			userService.update(user);
			// create a report entity

			this.queueReaderJob.requestBackroundWorkItem(reportInputEntity,
					subjectsForParseExperianReportObserver,
					"ExperianBureauService", "observeReport");

		} catch (Exception ex) {
			// compensate if queue is down
			try {
				parseExperianReportObserver.parse(reportInputEntity);
			} catch (Exception ex1) {
				throw new ConflictException("Report", ex1.toString(), ex1);
			}
		}
		return user;
	}

	/**
	 * This method set the report version.
	 * 
	 * @param reportInputEntiity
	 *            The report input Entity
	 * @param report
	 *            The Report
	 * @return the report
	 * @throws UnauthorizedException
	 *             The unauthorized exception.
	 */
	private Report checkOrSetReportVersion(
			ReportInputEntiity reportInputEntiity, Report report)
			throws UnauthorizedException {
		BoilerplateMap<String, Report> reportMap = this.reportService
				.getReports(reportInputEntiity.getUserId());
		int size = reportMap.size();
		report.setReportVersionEnum(ReportVersion.values()[size + 1]);
		return report;
	}
	/**
	 * @see IExperianService.fetchNextItem
	 */
	@Override
	public ReportInputEntiity fetchNextItem(String questionId,
			String answerPart1, String answerPart2)
			throws FactoryConfigurationError, Exception {
		ExternalFacingReturnedUser user = userService.get(RequestThreadLocal
				.getSession().getExternalFacingUser().getUserId(), false);
		ReportInputEntiity reportInputEntity = user.getReportInputEntitty();
		if (reportInputEntity == null) {
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity,
					"--- Method Name: " + " fetchNextItem --- Generic Error Message: No Experian Session found ",false);
			throw new NotFoundException("ReportInputEntity","No Experian Session found", null);
		}
		reportInputEntity.setStateEnum(State.Question);
		ExperianQuestionAnswer experianQuestionAnswer = null;
		// you can only call with empty question id for the first time.
		// so check the map
		if (questionId == null || (questionId.trim().equals(""))) {
			if (reportInputEntity.getQuestion().size() > 0) {
				pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
				// updates the experianStatus:(15-11-2016)
				experianStatusUpdate(reportInputEntity,
						"--- Method Name: " + " fetchNextItem  --- Generic Error Message: Question id is empty ",false);
				throw new ConflictException("Question", "Question id is empty",
						null);
			}
		} else {
			// You can only call a question and set of answers which exist in
			// the map.
			experianQuestionAnswer = reportInputEntity.getQuestion()
					.get(questionId);
			if (experianQuestionAnswer == null) {
				pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
				// updates the experianStatus:(15-11-2016)
				experianStatusUpdate(reportInputEntity, "--- Method Name: fetchNextItem " + "--- Generic Error Message: Question with id " + questionId + " not found",
						false);
				throw new NotFoundException("Question",
						"Question with id " + questionId + " not found", null);
			}

		}

		// fill the answer to the question
		String questionTemplate = null;
		if (questionId == null || questionId.equals("")) {
			questionTemplate = configurationManager
					.get("Experian_Question_First_Time_Body");
		} else {
			questionTemplate = configurationManager
					.get("Experian_Question_Body");
			experianQuestionAnswer.setOptionSet1Answer(answerPart1);
			experianQuestionAnswer.setOptionSet2Answer(answerPart2);
		}
		user.setReportInputEntitty(reportInputEntity);
		userService.update(user);
		
		String answerTemplate = (answerPart1 == null ? ""
				: answerPart1.replace(" ", "+")) + "%3A"
				+ (answerPart2 == null ? "" : answerPart2.replace(" ", "+"));
		questionTemplate = questionTemplate.replace("{answer}", answerTemplate);
		questionTemplate = questionTemplate.replace("{questionId}",
				questionId == null ? "" : questionId);
		questionTemplate = questionTemplate.replace("{stgOneHitId}",
				reportInputEntity.getStage1Id());
		questionTemplate = questionTemplate.replace("{stgTwoHitId}",
				reportInputEntity.getStage2Id());
		

		BoilerplateMap<String, BoilerplateList<String>> requestHeaders = createExperianQuestionRequestHeaders();

		HttpCookie jSessionIdCookie = new HttpCookie("JSESSIONID",
				reportInputEntity.getSessionId2());
		BoilerplateList<HttpCookie> requestCookies = new BoilerplateList();
		requestCookies.add(jSessionIdCookie);
		logger.logInfo("ExperianBureauService", "Experian_Question",
				"Request" + reportInputEntity.getUserId()
						+ reportInputEntity.getStage1Id(),
				questionTemplate);
	
		String requestBodyJava = createExperianQuestionRequest(configurationManager.get("Experian_Question_URL")+ questionTemplate, requestHeaders, requestCookies, questionTemplate);
		
		BoilerplateMap<String, BoilerplateList<String>> requestHeadersJava = new BoilerplateMap();
		BoilerplateList<String> headerValueJava = new BoilerplateList<String>();
		BoilerplateList<String> contentTypeHeaderValueJava = new BoilerplateList<String>();
		contentTypeHeaderValueJava.add("application/json;charset=UTF-8");
		requestHeadersJava.put("Content-Type", contentTypeHeaderValueJava);

		
		HttpResponse httpResponseJava = HttpUtility.makeHttpRequest(
				configurationManager.get("Experian_INITIATE_URL_JAVA"),
				requestHeadersJava, null, requestBodyJava, "POST");
		
		HttpResponse httpResponse = Base.fromJSON(
				httpResponseJava.getResponseBody(), HttpResponse.class);

		logger.logInfo("ExperianBureauService", "Experian_Question",
				"Response Status Code" + reportInputEntity.getUserId()
						+ reportInputEntity.getStage1Id(),
				Integer.toString(httpResponse.getHttpStatus()));
		
		if (httpResponse.getHttpStatus() != 200) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity,
					"--- Method Name: " + " fetchNextItem --- Response status: "+ httpResponse.getHttpStatus()
							+ "--- Generic Error Message: Issue in accessing server - Experian_CRQ_Request ",
					false);
			throw new PreconditionFailedException("Experian Server",
					"Issue in accessing server - Experian_CRQ_Request", null);
		}


		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> responseBodyMap = objectMapper
				.readValue(httpResponse.getResponseBody(), Map.class);
		// throws exception in case responseJson is not returned
		if (responseBodyMap.get("responseJson") == null) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			// updates the experianStatus:(23-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: "
					+ " fetchNextItem " + "--- Response status: "
					+ httpResponse.getHttpStatus()
					+ "--- Generic Error Message: "
					+ " responseJson is not returned"
					+ "--- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),
					true);
			throw new PreconditionFailedException("Experian Server",
					"responseJson is not returned "
							+ getErrorStringAndResponseJsonFields(
									responseBodyMap),
					null);
		}
		if (responseBodyMap.get("responseJson").equals("next") == true) {
			ExperianQuestionAnswer newExperianQuestionAnswer = new ExperianQuestionAnswer();
			Map<String, Object> questionToCustomerMap = (Map<String, Object>) responseBodyMap
					.get("questionToCustomer");
			List<String> optionSet1 = (List<String>) questionToCustomerMap
					.get("optionsSet1");
			for (String question : optionSet1) {
				newExperianQuestionAnswer.getOptionSet1().add(question);
			}
			List<String> optionSet2 = (List<String>) questionToCustomerMap
					.get("optionsSet2");
			for (String question : optionSet2) {
				newExperianQuestionAnswer.getOptionSet2().add(question);
			}
			String question = (String) questionToCustomerMap.get("question");
			newExperianQuestionAnswer.setQuestionText(question);
			String nextQuestionId = questionToCustomerMap.get("qid").toString();
			newExperianQuestionAnswer.setQuestionId(nextQuestionId);
			reportInputEntity.getQuestion().put(nextQuestionId,
					newExperianQuestionAnswer);
			reportInputEntity.setCurrentQuestion(newExperianQuestionAnswer);
			reportInputEntity
					.setQuestionCount(reportInputEntity.getQuestionCount() + 1);
			user.setReportInputEntitty(reportInputEntity);
			user.setUserState(MethodState.AuthQuestions);
			userService.update(user);
			RequestThreadLocal.getSession().addSessionAttribute("ExperianData",
					reportInputEntity);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: "
					+ " fetchNextItem " + "--- Response status: "
					+ httpResponse.getHttpStatus()
					+ "--- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),
					false);
		}

		if (responseBodyMap.get("responseJson").equals("passedReport")) {
			// we have a report
			// save this to the users context
			// and change state to next question
			reportInputEntity.setStateEnum(State.Report);
			String report = (String) responseBodyMap
					.get("showHtmlReportForCreditReport");
			MockMultipartFile file = new MockMultipartFile(
					"experianreport.html", "experianreport.html", "text/html",
					report.getBytes());
			FileEntity fileEntity = fileService.saveFile("ExperianReport",
					file);
			reportInputEntity.setReportFileEntity(fileEntity);
			// saves the experian report url in the corresponding user

			// Open the file
			String htmlFile = FileUtils.readFileToString(new File(
					configurationManager.get("RootFileDownloadLocation")
							+ fileEntity.getFileName()));

			// cut out the xml part from it
			int startingPOsition = htmlFile.indexOf("xmlResponse") + 21;
			String xmlFile = htmlFile.substring(startingPOsition,
					htmlFile.length());
			String reportNumber = getReportNumber(xmlFile);
			reportInputEntity.setReportNumber(reportNumber);

			user = observeReport(reportInputEntity, user);
			user.setExperianReportUrl(fileEntity.getFullFileNameOnDisk());
			user.setUserState(MethodState.ReportGenerated);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: "
					+ " fetchNextItem " + "--- Response status: "
					+ httpResponse.getHttpStatus()
					+ "--- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),
					false);
		}

		handleError(user, reportInputEntity,responseBodyMap,httpResponse);

		user.getUserMetaData().put("ExperianData", reportInputEntity.toJSON());
		user.setReportInputEntitty(reportInputEntity);
		userService.update(user);
		// publishUserStateToCRM(user);
		RequestThreadLocal.getSession().addSessionAttribute("ExperianData",
				reportInputEntity);
		return reportInputEntity;
	}
	
	/**
	 * This method handle the errors in response of experian request.
	 * @param user The user
	 * @param reportInputEntity The reportinput entity
	 * @param responseBodyMap The response map
	 * @param httpResponse The httpresponse of experian request
	 * @throws Exception throws in case of any error occurs
	 */
	private void handleError(ExternalFacingReturnedUser user,ReportInputEntiity reportInputEntity, 
			Map<String, Object> responseBodyMap, HttpResponse httpResponse) throws Exception{
		if (responseBodyMap.get("responseJson").equals("systemError") == true) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: fetchNextItem " + "--- Response status: "
					+ httpResponse.getHttpStatus()
					+ "--- Generic Error Message:  Issue in accessing server - Experian_CRQ_Request -  systemError --- Error String and Response Json: "+ getErrorStringAndResponseJsonFields(responseBodyMap),false);
			throw new PreconditionFailedException("Experian Server",
					"Issue in accessing server - Experian_CRQ_Request -  systemError",
					null);
		}

		if (responseBodyMap.get("responseJson").equals("error") == true) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: fetchNextItem " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: Issue in accessing server - Experian_CRQ_Request -  error --- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),false);
			throw new PreconditionFailedException("Experian Server",
					"Issue in accessing server - Experian_CRQ_Request -  systemError",
					null);
		}

		if (responseBodyMap.get("responseJson")
				.equals("creditReportEmpty") == true) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			reportInputEntity.setStateEnum(State.CreditReportEmpty);
			user.setUserState(MethodState.KYCPending);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: fetchNextItem " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: " + " creditReportEmpty --- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),
					false);
		}

		if (responseBodyMap.get("responseJson")
				.equals("inCorrectAnswersGiven") == true) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			reportInputEntity.setStateEnum(State.IncorrectAnswerGiven);
			user.setUserState(MethodState.KYCPending);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: "
					+ " fetchNextItem " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: " + " inCorrectAnswersGiven --- Error String and Response Json: "
					+ getErrorStringAndResponseJsonFields(responseBodyMap),
					false);
		}

		if (responseBodyMap.get("responseJson")
				.equals("insufficientQuestion") == true) {
			pushIntoQueueToSendKYCUploadSMS(reportInputEntity);
			reportInputEntity.setStateEnum(State.InsufficientQuestions);
			user.setUserState(MethodState.KYCPending);
			// updates the experianStatus:(15-11-2016)
			experianStatusUpdate(reportInputEntity, "--- Method Name: "
					+ " fetchNextItem " + "--- Response status: "+ httpResponse.getHttpStatus()+ "--- Generic Error Message: " + " insufficientQuestion --- Error String and Response Json: "+ getErrorStringAndResponseJsonFields(responseBodyMap),
					false);
		}
	}

	private String getReportNumber(String xmlFile)
			throws ParserConfigurationException, SAXException, IOException {
		xmlFile = xmlFile.replace("\"/>", "");
		xmlFile = xmlFile.replace("</body>", "");
		xmlFile = xmlFile.replace("</html>", "");
		xmlFile = StringEscapeUtils.unescapeHtml(xmlFile);

		DocumentBuilder documentBuilder = DocumentBuilderFactory
				.newInstance().newDocumentBuilder();
		InputSource inputSource = new InputSource();
		inputSource.setCharacterStream(new StringReader(xmlFile));

		Document doc = documentBuilder.parse(inputSource);

		// Normalize the XML Structure; It's just too important !!
		NodeList root = doc.getChildNodes();
		Node rootNode = getNode("InProfileResponse", root);
		Node creditProfileHeader = getNode("CreditProfileHeader",
				rootNode.getChildNodes());
		NodeList creditProfileHeaderNodes = creditProfileHeader
				.getChildNodes();
		// get report number
		String reportNumber = getNodeValue("ReportNumber",
				creditProfileHeaderNodes);
		return reportNumber;
	}
	/**
	 * This method create the headers for experian fetch next item request
	 * @return The required request headers
	 */
	private BoilerplateMap<String, BoilerplateList<String>> createExperianQuestionRequestHeaders() {
		BoilerplateMap<String, BoilerplateList<String>> requestHeaders = new BoilerplateMap();
		BoilerplateList<String> headerValue = new BoilerplateList<String>();
		headerValue.add(configurationManager.get("Experian_User_Agent"));
		requestHeaders.put("User-Agent", headerValue);

		headerValue = new BoilerplateList<String>();
		headerValue.add(configurationManager.get("Experian_Question_Accept"));
		requestHeaders.put("Accept", headerValue);

		headerValue = new BoilerplateList<String>();
		headerValue.add(
				configurationManager.get("Experian_Question_Content_Type"));
		requestHeaders.put("Content-Type", headerValue);
		return requestHeaders;
	}

	/**
	 * This method push reportInputEntity Into Queue To Send KYC Upload SMS
	 * 
	 * @param reportInputEntity
	 *            The reportInputEntity
	 * @throws Exception  throws in case of queue down
	 */
	private void pushIntoQueueToSendKYCUploadSMS(
			ReportInputEntiity reportInputEntity)
			throws Exception {
		ReportInputEntiity ReportInputEntiityClone = null;
		ReportInputEntiityClone = (ReportInputEntiity) reportInputEntity
				.clone();
		ExternalFacingReturnedUser externalFacingReturnedUser = this.userService
				.get(reportInputEntity.getUserLoginId());

		if (reportInputEntity.isDontSendKycSms() == false
				|| !(reportInputEntity.isDontSendKycSms())) {
			queueReaderJob.requestBackroundWorkItem(externalFacingReturnedUser,
					subjectsForExperianKYCUpload, "ExperianBureauService",
					"pushIntoQueueToSendKYCUploadSMS");

		}

	}
	/**
	 * This method create the request for experian question api
	 * @param url The experian question api url
	 * @param requestHeaders The request headers
	 * @param requestCookies The request cookies
	 * @param questionTemplate The question template
	 * @return the required request
	 */
	public String createExperianQuestionRequest(String url,BoilerplateMap<String, 
			BoilerplateList<String>> requestHeaders,BoilerplateList<HttpCookie> 
	requestCookies,String questionTemplate){
		String request = configurationManager.get("JAVA_HTTP_REQUEST_TEMPLATE");
		request = request.replace("@url",url);
		request = request.replace("@requestHeaders", requestHeaders.toString());
		request = request.replace("@requestBody", questionTemplate);
		request = request.replace("@requestCookies", requestCookies.toString());
		request = request.replace("@method", "POST");
		return request;
	}

}
