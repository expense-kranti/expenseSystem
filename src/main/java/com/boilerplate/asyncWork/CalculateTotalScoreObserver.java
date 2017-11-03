package com.boilerplate.asyncWork;

import org.springframework.beans.factory.annotation.Autowired;

import com.boilerplate.database.interfaces.IRedisAssessment;
import com.boilerplate.framework.Logger;
import com.boilerplate.framework.RequestThreadLocal;
import com.boilerplate.java.collections.BoilerplateList;
import com.boilerplate.java.entities.AssessmentEntity;
import com.boilerplate.java.entities.PublishEntity;
import com.boilerplate.java.entities.ScoreEntity;

/**
 * This class calculate user total score and update the details into data store
 * 
 * @author shiva
 *
 */
public class CalculateTotalScoreObserver implements IAsyncWorkObserver {
	
	/**
	 * CalculateTotalScoreObserver logger
	 */
	private static Logger  logger = Logger.getInstance(CalculateTotalScoreObserver.class);
	
	@Autowired
	com.boilerplate.jobs.QueueReaderJob queueReaderJob;
	
	/**
	 * This sets the queue reader job
	 * @param queueReaderJob The queue reader jon
	 */
	public void setQueueReaderJob(com.boilerplate.jobs.QueueReaderJob queueReaderJob){
		this.queueReaderJob = queueReaderJob;
	}
	
	/**
	 * This is the new instance of redis assessment
	 */
	@Autowired
	IRedisAssessment redisAssessment;

	/**
	 * This method set the redisAssessment
	 * 
	 * @param redisAssessment
	 *            the redisAssessment to set
	 */
	public void setRedisAssessment(IRedisAssessment redisAssessment) {
		this.redisAssessment = redisAssessment;
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
	public void setConfigurationManager(com.boilerplate.configurations.ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}
	
	private BoilerplateList<String> subjects = null;

	/**
	 * @see IAsyncWorkObserver.observe
	 */
	@Override
	public void observe(AsyncWorkItem asyncWorkItem) throws Exception {
		AssessmentEntity assessmentEntity = (AssessmentEntity) asyncWorkItem.getPayload();
		this.calculateTotalScore(assessmentEntity);
		this.calculateMonthlyScore(assessmentEntity);
	}

	/**
	 * This method is used to calculate the user total score
	 * 
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	public void calculateTotalScore(AssessmentEntity assessmentEntity) {
		// Get total score
		ScoreEntity scoreEntity = redisAssessment.getTotalScore(assessmentEntity.getUserId());
		// If score is not null
		if (scoreEntity != null) {
			this.updateTotalScore(scoreEntity, assessmentEntity);
		} else {
			this.createNewTotalScore(assessmentEntity);
		}
	}

	/**
	 * This method is used to update the user total score
	 * 
	 * @param scoreEntity
	 *            this parameter contains the user monthly score,user obtained
	 *            score and max score
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	private void updateTotalScore(ScoreEntity scoreEntity, AssessmentEntity assessmentEntity) {
		// Calculate max score
		Float maxScore = Float.valueOf(scoreEntity.getMaxScore()) + Float.valueOf(assessmentEntity.getMaxScore());
		// Calculate total score
		Float obtainedScore = Float.valueOf(scoreEntity.getObtainedScore())
				+ Float.valueOf(assessmentEntity.getObtainedScore());
		// Set max score
		scoreEntity.setMaxScore(String.valueOf(maxScore));
		// Set the obtained score
		scoreEntity.setObtainedScore(String.valueOf(obtainedScore));
		// set rank
		scoreEntity.setRank(calculateRank(obtainedScore));
		// Save total score
		redisAssessment.saveTotalScore(scoreEntity);
	}

	/**
	 * This method is used to create a new user total score detail
	 * 
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	private void createNewTotalScore(AssessmentEntity assessmentEntity) {
		// New instance of score entity
		ScoreEntity scoreEntity = new ScoreEntity();
		// Set max score
		scoreEntity.setMaxScore(assessmentEntity.getMaxScore());
		// Set the obtained score
		scoreEntity.setObtainedScore(assessmentEntity.getObtainedScore());
		// Set user id
		scoreEntity.setUserId(assessmentEntity.getUserId());
		// set rank
		scoreEntity.setRank(calculateRank(Float.parseFloat(assessmentEntity.getObtainedScore())));
		// Save total score
		redisAssessment.saveTotalScore(scoreEntity);
	}

	/**
	 * This method is used to calculate the user monthly score
	 * 
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	public void calculateMonthlyScore(AssessmentEntity assessmentEntity) {
		// Get total score
		ScoreEntity scoreEntity = redisAssessment.getMonthlyScore(assessmentEntity.getUserId());
		// If score is not null
		if (scoreEntity != null) {
			this.updateMonthlyScore(scoreEntity, assessmentEntity);
		} else {
			this.createNewMonthlyScore(assessmentEntity);
		}
		this.publishToCRM(assessmentEntity);
	}

	/**
	 * This method is used to create a new user monthly score detail
	 * 
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	private void createNewMonthlyScore(AssessmentEntity assessmentEntity) {
		// New instance of score entity
		ScoreEntity scoreEntity = new ScoreEntity();
		// Set max score
		scoreEntity.setMaxScore(assessmentEntity.getMaxScore());
		// Set the obtained score
		scoreEntity.setObtainedScore(assessmentEntity.getObtainedScore());
		// Set user id
		scoreEntity.setUserId(assessmentEntity.getUserId());
		// set rank
		scoreEntity.setRank(calculateRank(Float.parseFloat(assessmentEntity.getObtainedScore())));
		// Save monthly score
		redisAssessment.saveMonthlyScore(scoreEntity);
		
	}

	/**
	 * This method is used to update the user monthly score
	 * 
	 * @param scoreEntity
	 *            this parameter contains the user monthly score,user obtained
	 *            score and max score
	 * @param assessmentEntity
	 *            this parameter contains the assessment data ,assessment data
	 *            like assessment id, assessment section,assessment
	 *            questions,assessment score,obtained score etc.
	 */
	private void updateMonthlyScore(ScoreEntity scoreEntity, AssessmentEntity assessmentEntity) {
		// Calculate max score
		Float maxScore = Float.valueOf(scoreEntity.getMaxScore()) + Float.valueOf(assessmentEntity.getMaxScore());
		// Calculate total score
		Float obtainedScore = Float.valueOf(scoreEntity.getObtainedScore())
				+ Float.valueOf(assessmentEntity.getObtainedScore());
		// Set max score
		scoreEntity.setMaxScore(String.valueOf(maxScore));
		// Set the obtained score
		scoreEntity.setObtainedScore(String.valueOf(obtainedScore));
		// set rank
		scoreEntity.setRank(calculateRank(obtainedScore));
		// Save monthly score
		redisAssessment.saveMonthlyScore(scoreEntity);
	}

	/**
	 * This method set the rank of the user.
	 * 
	 * @param score
	 *            This is the current user score.
	 * @return The rank of the user.
	 */
	private String calculateRank(Float score) {
		String rank = "";
		if (score > 0 && score < 50) {
			rank = configurationManager.get("Rank1");
		} else if (score >= 50 && score < 100) {
			rank = configurationManager.get("Rank2");
		} else if (score >= 100 && score < 500) {
			rank = configurationManager.get("Rank3");
		} else if (score >= 500 && score < 1000) {
			rank = configurationManager.get("Rank4");
		} else if (score >= 1000 && score < 1500) {
			rank = configurationManager.get("Rank5");
		} else if (score >= 1500 && score < 2000) {
			rank = configurationManager.get("Rank6");
		} else if (score >= 2000 && score < 2500) {
			rank = configurationManager.get("Rank7");
		} else if (score >= 2500 && score < 3000) {
			rank = configurationManager.get("Rank8");
		} else if (score >= 3000) {
			rank = configurationManager.get("Rank9");
		}
		return rank;
	}
	
	private void publishToCRM(AssessmentEntity assessmentEntity){
		Boolean isPublishReport = Boolean.valueOf(configurationManager.get("Is_Publish_Report"));
		if(isPublishReport){
			PublishEntity publishEntity =  this.
					createPublishEntity("CalculateTotalScoreObserver.publishToCRM", 
							configurationManager.get("AKS_Assessment_Publish_Method")
					, configurationManager.get("AKS_Assessment_Publish_Subject"),
					assessmentEntity,configurationManager.get("AKS_Assessment_Publish_URL"),
					configurationManager.get("AKS_Assessment_Publish_Template"),
					configurationManager.get("AKS_Assessment_Dynamic_Publish_Url"));
			if(subjects == null){
				subjects = new BoilerplateList<>();
				subjects.add(configurationManager.get("AKS_PUBLISH_SUBJECT"));
			}
			try{
				logger.logInfo("CalculateTotalScoreObserver", "publishToCRM", "Publishing report", "publish assessment status"+assessmentEntity.getId());
				queueReaderJob.requestBackroundWorkItem(publishEntity, subjects , "CalculateTotalScoreObserver", "publishToCRM", configurationManager.get("AKS_PUBLISH_QUEUE"));
			}
			catch (Exception exception){
				logger.logError("CalculateTotalScoreObserver", "publishToCRM", "queueReaderJob catch block", "Exception :" + exception);
			}
		}
	}
	/**
	 * This method creates the publish entity.
	 * @param method the publish method
	 * @param publishMethod the publish method
	 * @param publishSubject the publish subject
	 * @param returnValue the object
	 * @param url the publish url
	 * @return the publish entity
	 */
	private PublishEntity createPublishEntity(String method,String publishMethod,String publishSubject,
			Object returnValue, String url, String publishTemplate, String isDynamicPublishURl){
		PublishEntity publishEntity = new PublishEntity();
		publishEntity.setInput(new Object[0]);
		publishEntity.setMethod(method);
		publishEntity.setPublishMethod(publishMethod);
		publishEntity.setPublishSubject(publishSubject);
		publishEntity.setReturnValue(returnValue);
		publishEntity.setUrl(url);
		publishEntity.setDynamicPublishURl(Boolean.parseBoolean(isDynamicPublishURl));
		publishEntity.setPublishTemplate(publishTemplate);
		return publishEntity;
	}
}