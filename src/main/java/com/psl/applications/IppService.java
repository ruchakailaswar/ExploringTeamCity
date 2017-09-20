package com.psl.applications;

import static org.eclipse.stardust.engine.core.spi.dms.RepositoryIdUtils.stripRepositoryId;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.error.AccessForbiddenException;
import org.eclipse.stardust.common.error.InvalidValueException;
import org.eclipse.stardust.common.error.ObjectNotFoundException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.model.Activity;
import org.eclipse.stardust.engine.api.model.DataPath;
import org.eclipse.stardust.engine.api.model.ImplementationType;
import org.eclipse.stardust.engine.api.model.ModelParticipant;

import org.eclipse.stardust.engine.api.query.ActivityInstanceQuery;
import org.eclipse.stardust.engine.api.query.DataFilter;
import org.eclipse.stardust.engine.api.query.DescriptorPolicy;
import org.eclipse.stardust.engine.api.query.ProcessInstanceFilter;
import org.eclipse.stardust.engine.api.query.ProcessInstanceQuery;
import org.eclipse.stardust.engine.api.query.ProcessInstances;
import org.eclipse.stardust.engine.api.runtime.ActivityInstance;
import org.eclipse.stardust.engine.api.runtime.ActivityInstanceState;
import org.eclipse.stardust.engine.api.runtime.AdministrationService;
import org.eclipse.stardust.engine.api.runtime.DmsUtils;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.DocumentInfo;
import org.eclipse.stardust.engine.api.runtime.DocumentManagementService;
import org.eclipse.stardust.engine.api.runtime.DocumentManagementServiceException;
import org.eclipse.stardust.engine.api.runtime.Folder;
import org.eclipse.stardust.engine.api.runtime.ProcessInstance;
import org.eclipse.stardust.engine.api.runtime.ProcessInstanceState;
import org.eclipse.stardust.engine.api.runtime.QueryService;
import org.eclipse.stardust.engine.api.runtime.ServiceFactory;
import org.eclipse.stardust.engine.api.runtime.ServiceFactoryLocator;
import org.eclipse.stardust.engine.api.runtime.User;
import org.eclipse.stardust.engine.api.runtime.UserInfo;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;
import org.eclipse.stardust.engine.core.runtime.beans.AbortScope;
import org.eclipse.stardust.engine.extensions.dms.data.DocumentType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.psl.beans.ApplicationConstants;

public class IppService {

	private ServiceFactory serviceFactory;
	private WorkflowService workflowService;
	private DocumentManagementService documentService;
	private AdministrationService administrationService;
	private QueryService queryService;

	//public static final String META_DATA_SCHEME_NO = "SchemeNo";
	//public static final String META_DATA_MEMBER_NO = "MemberNo";
	//public static final String META_DATA_REQUEST_TYPE = "RequestType";
	//public static final String META_DATA_DOCUMENT_TYPES = "DocumentType";



	public QueryService getQueryService() {
		return getServiceFactory().getQueryService();
	}

	public void setQueryService(QueryService queryService) {
		this.queryService = queryService;
	}



	private String ippPassword;
	private String ippUser;
	//private static final String ROOT_FOLDER = "/liberty";
	//private static final String SECURITY_DOCUMENTS_PATH = "/securityDocuments";
	//private static final String PROCESS_ATTACHMENTS = "PROCESS_ATTACHMENTS";
	static final Logger LOG = LogManager.getLogger(IppService.class);

	public IppService() {
		/*this.serviceFactory = getServiceFactory();
		this.workflowService = serviceFactory.getWorkflowService();
		this.documentService = serviceFactory.getDocumentManagementService();*/
	}

	public DocumentManagementService getDocumentManagementService() {
		return getServiceFactory().getDocumentManagementService();
		//return documentService;
	}

	public void setDocumentService(DocumentManagementService documentService) {
		this.documentService = documentService;
	}

	public AdministrationService getAdministrationService() {
		return getServiceFactory().getAdministrationService();
		//return administrationService;
	}

	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
	}

	public String getIppPassword() {
		return ippPassword;
	}

	public void setIppPassword(String ippPassword) {
		this.ippPassword = ippPassword;
	}

	public String getIppUser() {
		return ippUser;
	}

	public void setIppUser(String ippUser) {
		this.ippUser = ippUser;
	}

	private ServiceFactory getServiceFactory() {

		serviceFactory = ServiceFactoryLocator.get(ippUser, ippPassword);

		return serviceFactory;
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public WorkflowService getWorkflowService() {

		return getServiceFactory().getWorkflowService();
		
	}

	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	public Object getProcessData(Long processOID, String dataID) {

		try {
			Object object = getWorkflowService().getInDataPath(processOID, dataID);
			return object;
		} catch (ObjectNotFoundException e) {
			LOG.warn("Could not find Data Path " + dataID + " on scope process Instance " + processOID);
			return null;
		} catch (InvalidValueException e) {
			LOG.warn("Could not get data for Data Path " + dataID + " on scope process Instance " + processOID);
			return null;
		}
	}

	public List<Document> getAttachedDocuments(Long processOid) {

		List<Document> attachedDocs = new ArrayList<Document>();
		try {
			List<Document> processAttachments = null;

			Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());

			processAttachments = (List<Document>) processData;

			if (processAttachments != null && !processAttachments.isEmpty()) {
				LOG.info("GetDocumentListService NEW REST API : processAttachments for processOID " + processOid
						+ " are " + processAttachments.size());
				for (Document processAttachment : processAttachments) {
					LOG.info("GetDocumentListService NEW REST API : Document Detail -- " + processAttachment.getId()
					+ " " + processAttachment.getName() + " " + processAttachment.getDateCreated());
					LOG.info("GetDocumentListService NEW REST API : Fetching a processAttachments for processOID "
							+ processOid);
					attachedDocs.add(processAttachment);
				}
			} else {
				LOG.info("GetDocumentListService NEW REST API : processAttachments for processOID " + processOid
						+ " are empty!");
			}
		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception getAttachedDocuments -- " + e.getMessage());
			LOG.info("GetDocumentListService NEW REST API : Exception getAttachedDocuments -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception getAttachedDocuments -- " + e.getCause());

			return attachedDocs;
		}
		return attachedDocs;
	}

	public Document saveDocumentinIpp(byte[] documentContent, String filename, String contentType, long processOid,
			Map properties) {
		Document document = this.createDocument(documentContent, filename, contentType, processOid, properties);
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		this.addDocumentsToProcessAttachments(documents, processOid);
		return document;
	}

	public Document createDocument(byte[] documentContent, String filename, String contentType, long processOid,
			Map<String,Object> properties) {

		LOG.info("Save Document in IPP : IPP SERVICE " + processOid);
		String defaultPath = DmsUtils.composeDefaultPath(processOid, new Date());
		defaultPath = ApplicationConstants.ROOT_FOLDER.getValue() + defaultPath;
		DocumentInfo documentInfo = DmsUtils.createDocumentInfo(filename);
		Folder folder = DmsUtils.ensureFolderHierarchyExists(defaultPath, getDocumentManagementService());
		if (properties != null) {
			documentInfo.getProperties().putAll(properties);
		}
		documentInfo.setContentType(contentType);

		Document document = null;
		try {
			document = getDocumentManagementService().createDocument(defaultPath, documentInfo, documentContent, null);
		} catch (DocumentManagementServiceException e) {
			LOG.warn("Document Management service exception: " + e);
			document = checkForDuplicateFile(documentContent, filename, defaultPath, documentInfo, document, e);

		}
		return document;
	}

	private Document checkForDuplicateFile(byte[] documentContent, String filename, String defaultPath,
			DocumentInfo documentInfo, Document document, DocumentManagementServiceException e)
					throws DocumentManagementServiceException {
		Folder folder;
		// Check if the exception is caused by a "file exists exception"
	
		Document document2 = getDocumentManagementService().getDocument(defaultPath + "/" + filename);
		if (document2 != null) {
			defaultPath = defaultPath + new Date().getTime();
			folder = DmsUtils.ensureFolderHierarchyExists(defaultPath, getDocumentManagementService());
			try {
				document = getDocumentManagementService().createDocument(defaultPath, documentInfo, documentContent,
						null);

			} catch (DocumentManagementServiceException e1) {
				LOG.warn("Document Management service exception: " + e);
				throw e;
			}
		} else {
			throw e;
		}
		return document;
	}

	public void addDocumentsToProcessAttachments(List<Document> documents, long processOid)
			throws DocumentManagementServiceException {

		try {
			Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
			List attachments = null;
			if (processData == null) {
				LOG.info("Process Attachments empty");
				attachments = new ArrayList();

			} else {
				LOG.info("Process Attachments found: " + processData.getClass());
				attachments = (List) processData;
			}
			attachments.addAll(documents);
			setProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), attachments);
		} catch (DocumentManagementServiceException e) {
			LOG.warn("Document Management service exception: " + e);
			throw e;
		}
	}





	public void setProcessData(Long processOID, String dataID, Object data) throws InvalidValueException {

		try {
			getWorkflowService().setOutDataPath(processOID, dataID, data);

		} catch (ObjectNotFoundException e) {
			LOG.warn("Could not find Data Path " + dataID + " on scope process Instance " + processOID);
			throw e;
		} catch (InvalidValueException e) {
			LOG.warn("Could not set data " + data + " for Data Path " + dataID + " on scope process Instance "
					+ processOID);
			throw e;
		}
	}

	public Response prepareJSONData(List<Document> docsList) {
		Calendar cal = Calendar.getInstance();
		String formatedDate = null;

		JsonObject jo = null;
		JsonArray ja = new JsonArray();
		JsonObject mainObj = new JsonObject();
		try {
			for (Document doc : docsList) {

				jo = new JsonObject();
				jo.addProperty("docName", doc.getName());
				jo.addProperty("View", doc.getId());
				/*if(doc.getDateCreated() !=null){
					cal.setTime(doc.getDateCreated());
					formatedDate = cal.get(Calendar.DATE) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
				}*/
				jo.addProperty("createdDate", doc.getDateCreated() == null ? " " : doc.getDateCreated().toString());
				//jo.addProperty("createdDate", doc.getDateCreated() == null ? " " : formatedDate);
				// jo.addProperty("lastModifiedDate", doc.getDateLastModified()
				// == null ? " " : doc.getDateLastModified().toString());
				//jo.addProperty("version", doc.getRevisionName() == null ? " " : doc.getRevisionName());
				jo.addProperty("version", doc.getRevisionName() == null ? "1.0" : (doc.getRevisionName().equalsIgnoreCase("UNVERSIONED")? "1.0":doc.getRevisionName()));
				jo.addProperty("docType", getDocumentTypeString(doc));
			    //LOG.info("Properties :" + doc.getProperties());
			    try{
				jo.addProperty("MetaDataDocVersion",doc.getProperties().get(ApplicationConstants.META_DATA_MEMBER_NO.getValue()).toString());
			    }catch(Exception e){
			    	jo.addProperty("MetaDataDocVersion","memberno");
			    }
				//jo.addProperty("versionComment", doc.getRevisionComment());
				ja.add(jo);
			}

			mainObj.add("AvlClaimDocumentsSD", ja);
		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData  -- " + e);
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getCause());
			e.printStackTrace();
			return Response.ok("JSON Error!").build();
		}

		return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE).header("Access-Control-Allow-Origin", "*").build();
	}

	public String getDocumentTypeString(Document doc) {

		Map<String, Object> props = doc.getProperties();
		LOG.info("GetDocList Rest Service - Document Types properties - " + props);
		List<String> docTypes = new ArrayList<String>();
		if (props != null && props.size() > 0) {
			docTypes = (List<String>) props.get("DocumentType");
			/*
			 * if(docTypes != null && docTypes.size() > 0){ LOG.info(
			 * "GetDocList Rest Service - Document Types - "
			 * +docTypes.toString()); for(String s : docTypes) LOG.info(
			 * "GetDocList Rest Service - Document Types - "+s); }
			 */
		}
		return StringUtils.isNotEmpty(getCSVS(docTypes)) ? getCSVS(docTypes) : "Unassigned";
	}

	public static final String getCSVS(Collection<String> list) {
		StringBuilder sb = new StringBuilder();

		if (list != null && !list.isEmpty()) {
			for (String type : list) {
				sb.append(type).append(",");
			}
			return sb.delete(sb.length() - 1, sb.length()).toString();
		}
		return "";
	}



	/*
	 * 
	 * 
	 * 
	 *This is to return the byte array of any JCR Document with the ID 
	 * 
	 * 
	 *
	 */


	public String getDocumentContent(String documentID){

		byte[] docContent = null;
		String fileName = null;
		Document doc = null;
		try{
			doc = getDocumentManagementService().getDocument(documentID);
			
			docContent = getDocumentManagementService().retrieveDocumentContent(documentID);
			
			LOG.info(doc.getRepositoryId());
			LOG.info(doc.getSize());
			LOG.info(doc.getContentType());
			LOG.info(documentID);
			fileName = doc.getName();
		}catch(Exception e){
			LOG.info("Error in retrieving document content with document ID " + documentID);
			LOG.info("Error stack trace is" + e.getStackTrace());
			String response = "{\"fileName\":"+null+",\n \"documentContent\":"+null+",\n \"error\":"+"\"No Document Found with the requested DOC ID !!\""+"}";
			return response;
		}
		int[] arr = new int[docContent.length];
		int i=0;
		for(byte  b : docContent){
	   		arr[i++] = (0x000000FF) & b;
	 		//arr[i++] = b.intValue();
	   		
	   	}
		
		String response = "{\"fileName\":\""+fileName+"\",\n \"documentContent\":"+Arrays.toString(arr)+",\n \"error\":"+null+"}";
		LOG.debug("The Doc Content is :" + Arrays.toString(arr));
		
		return response ;
	}



	public List<Map<String,String>> getActivitiesDetails(long processOid,boolean onlyInteractive){

		String activityName = null;
		String modelParticipantName = null;
		String activityState = null;
		String startTime = null;
		String lastModificationTime = null;
		String userName = null;
		List<Map<String,String>> activitiesList = new ArrayList<Map<String,String>>();


		ActivityInstanceQuery activityQuery = ActivityInstanceQuery.findForProcessInstance(processOid);
		activityQuery.where(new ProcessInstanceFilter(processOid));
		List<ActivityInstance> activityList = getQueryService().getAllActivityInstances(activityQuery);

		for(ActivityInstance ai: activityList){

			Map<String,String> activityDetailsMap = new HashMap<String,String>();

			Activity activity = ai.getActivity();

			if(activity != null && (onlyInteractive?activity.isInteractive():true)){
				ImplementationType type = activity.getImplementationType();
				if(type != null){
				
					if(type.getName().equalsIgnoreCase(ImplementationType.Application.getName())||type.isSubProcess()){

						activityName = activity.getName();

						ModelParticipant modelParticipant = activity.getDefaultPerformer();
						modelParticipantName =(modelParticipant != null)? modelParticipant.getName():null;	

						UserInfo userInfo = ai.getPerformedBy();
						userName = (userInfo != null)?userInfo.getFirstName()+" "+ userInfo.getLastName():null;

						ActivityInstanceState state = ai.getState();
						if(state != null){
							activityState= state.getName();
						}

						if(ai.getStartTime() != null){
							startTime = ai.getStartTime().toString();

						}
						if(ai.getLastModificationTime() != null){
							lastModificationTime = ai.getLastModificationTime().toString();
						}


						activityDetailsMap.put("activityInstanceOid", new Long(ai.getOID()).toString());
						activityDetailsMap.put("activityName", activityName);
						activityDetailsMap.put("modelParticipant", modelParticipantName);
						activityDetailsMap.put("performedBy", userName);
						activityDetailsMap.put("state", activityState);
						activityDetailsMap.put("startTime",startTime);
						activityDetailsMap.put("lastModificationTime", lastModificationTime);

						activitiesList.add(activityDetailsMap);
					}
				}
			}

		}
		
		LOG.info("Process Deatils :"+ activitiesList);
		return activitiesList;
	}


	public Map<String,String> getProcessDetails(long processOid){


		Map<String,String> processDetails = new HashMap<String,String>();

		ProcessInstance processInstance = getWorkflowService().getProcessInstance(processOid);
		if(processInstance != null){

			processDetails.put("processName", processInstance.getProcessName());
			processDetails.put("rootProcessOid", new Long(processInstance.getRootProcessInstanceOID()).toString());
			LOG.info("Process Deatils :"+ processDetails);
			
			ProcessInstanceState processState = processInstance.getState();
			String processStateName = (processState != null)?processState.getName():null;
			processDetails.put("processState", processStateName);
			LOG.info("Process Deatils :"+ processDetails);
			
			User user = processInstance.getStartingUser();
			String userName = (user != null)?user.getName():null;
			processDetails.put("startingUser", userName);
			LOG.info("Process Deatils :"+ processDetails);
			
			processDetails.put("startTime", processInstance.getStartTime().toString());
			LOG.info("Process Deatils :"+ processDetails);
			if(!(processState.getValue()==ProcessInstanceState.ACTIVE)){
			processDetails.put("endTime", processInstance.getTerminationTime().toString());
			}
			LOG.info("Process Deatils :"+ processDetails);
		}

		return processDetails;	
	}


	public JsonObject parseJSONObject(String input){
		JsonObject jsonObject = null;
		JsonParser jp=new JsonParser();
		JsonElement parsedData = jp.parse(input);

		if(parsedData != null && parsedData.isJsonObject()){
			jsonObject = parsedData.getAsJsonObject();
		}
		return jsonObject;
	}


	public Document updateNonSecureDocument(String docId,long processOid, Map properties) {

		Document document = getDocumentManagementService().getDocument(docId);
		document.getProperties().putAll(properties);

		//document.setDocumentType(new DocumentType("sample", "DocumentType"));
		LOG.info("updateNonsecureDocument: " + docId + " processOid: " + processOid + " document path: "
				+ document.getPath() + "Content- type :"+document.getContentType()+"Properties : "+document.getProperties());
		LOG.info("updateNonsecureDocument: stripRepositoryId: " + stripRepositoryId(docId));

		if (document.getPath().contains(ApplicationConstants.SECURITY_DOCUMENTS_PATH.getValue())) {
			String defaultPath = DmsUtils.composeDefaultPath(processOid, new Date());
			defaultPath = ApplicationConstants.ROOT_FOLDER.getValue() + defaultPath;
			defaultPath = DmsUtils.ensureFolderHierarchyExists(defaultPath, getDocumentManagementService()).getId();
			document = getDocumentManagementService().updateDocument(document, true, "updated meta data", "", false);
			document = getDocumentManagementService().moveDocument(document.getId(), defaultPath);
		} else {
			document = getDocumentManagementService().updateDocument(document, true, "updated meta data", "", false);
		}

		LOG.info("Updated Document properties  : " +document.getProperties() );
		Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
		List<Document> attachments = null;
		if (processData == null) {
			LOG.info("Process Attachments empty");
			attachments = new ArrayList<Document>();

		} else {
			LOG.info("Process Attachments found: " + processData.getClass() + " attachmenmts details :" + (List) processData );
			attachments = (List) processData;
		}

		Iterator<Document> itr = attachments.iterator();
		while(itr.hasNext()){
			Document doc = itr.next();
			if (stripRepositoryId(doc.getId()).equalsIgnoreCase(stripRepositoryId(document.getId()))) {
				// remove old document
				LOG.info("Removind old document , document details are : description -> " + doc.getDescription()+" properties : " + doc.getProperties());
				itr.remove();
				break;
			}
		}
		LOG.info("After removing : " + processData.getClass() + " attachmenmts details :" + (List) processData );
		attachments.add(document);
		LOG.info("After adding : " + processData.getClass() + " attachmenmts details :" + (List) processData );
		LOG.info("Added document with new document type as : document types "+ document.getDocumentType());
		LOG.info("Added Document properties  : " +document.getProperties() );
		//DocumentType dt = document.getDocumentType();

		//LOG.info("Document type : ID : "+ dt.getDocumentTypeId() + " and schema location : "+dt.getSchemaLocation());
		LOG.info("Setting process attachments : List of attachments "+ attachments);

		setProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), attachments);
		return document;
	}

	public Map<String, Object> populateDocumentProperties(String schemeNo,String memberNo, String workType, String docTypes){
		Map<String, Object> metaDataMap = new HashMap<String, Object>();


		String[] docTypeArray = docTypes.split("\\|");
		LOG.info("Populating Document properties : DOC Type Array  : " + docTypeArray);
		List<String> docTypeList = Arrays.asList(docTypeArray);
		LOG.info("Populating Document properties : DOC Type List  : " + docTypeList);

		metaDataMap.put(ApplicationConstants.META_DATA_SCHEME_NO.getValue(), schemeNo);
		metaDataMap.put(ApplicationConstants.META_DATA_MEMBER_NO.getValue(), memberNo);
		metaDataMap.put(ApplicationConstants.META_DATA_REQUEST_TYPE.getValue(), workType);
		metaDataMap.put(ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue(), docTypeList);


		return metaDataMap;
	}
	
	// not being used
	public void deleteDocuments(String docId){
		LOG.info("Inside IPP Service. Delete Document method. Document Id -: " + docId);
		getDocumentManagementService().removeDocument(docId);
	}
	
	public void removeDocument(String docId,long processOid){
		
		Document document = getDocumentManagementService().getDocument(docId);
		List<Document> processAttachments = null;
        List<Document> attachedDocs = new ArrayList<Document>();
		Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());

		processAttachments = (List<Document>) processData;
		attachedDocs.addAll(processAttachments);
		
		for(Document doc :attachedDocs){
			if(doc.getId().equalsIgnoreCase(docId)){
				LOG.info("Document : docId : "+doc.getId());
				processAttachments.remove(doc);
			}
		}
		LOG.info("Size of process attachment :"+processAttachments.size());
		
		setProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), processAttachments);
	}
	
	
	
	
	
	// Updated by prerna
	
    public ProcessInstances fetchProcessInstances(JsonObject jsonObject) {

        String processId = null;
        JsonObject mainObj;

        processId = jsonObject.get("processId").getAsString();

        ProcessInstanceQuery piQuery = ProcessInstanceQuery.findForProcess(processId, false);
        piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);
        jsonObject.remove("processId");
        Set<Map.Entry<String, JsonElement>> jsonMap = jsonObject.entrySet();

        LOG.info("inside getProcessDataPaths  method : Process ID : " + processId);

        for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

              String[] dataElement = mapEntry.getKey().split("\\.");
              Number dataValue;
              if (dataElement.length > 1) {

                    if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
                          dataValue = mapEntry.getValue().getAsJsonPrimitive().getAsNumber();
                          if (dataValue instanceof Integer || dataValue instanceof Long) {
                                piQuery.where(
                                            DataFilter.isEqual(dataElement[0], dataElement[1], mapEntry.getValue().getAsLong()));
                          } else if (dataValue instanceof Float || dataValue instanceof Double) {
                                piQuery.where(
                                            DataFilter.isEqual(dataElement[0], dataElement[1], mapEntry.getValue().getAsDouble()));
                          }
                    } else {
                          piQuery.where(
                                      DataFilter.isEqual(dataElement[0], dataElement[1], mapEntry.getValue().getAsString()));

                    }

              } else {
                    if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
                          dataValue = mapEntry.getValue().getAsJsonPrimitive().getAsNumber();
                          if (dataValue instanceof Integer || dataValue instanceof Long) {
                                piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsLong()));
                          } else if (dataValue instanceof Float || dataValue instanceof Double) {
                                piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsDouble()));
                          }
                    } else {
                          piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsString()));

                    }

              }

        }

        ProcessInstances pis = getQueryService().getAllProcessInstances(piQuery);

        LOG.info("Process Instance fetched  - Count : " + pis.getSize());

        return pis;
  }



/**
  * @param pis
  * @param setDataPath
  * @param dataPathJson
  * @return
  * Method by Prerna
  */
  public JsonObject fetchAndSetProcessDataPaths(ProcessInstances pis, boolean setDataPath, JsonObject dataPathJson) {
        JsonObject jo = null;
        JsonArray ja = new JsonArray();
        JsonObject mainObj = new JsonObject();
        String dataPathId = null;
        String datPathValue = null;
        long processInstanceOID = 0L;

        for (Iterator iterator = pis.iterator(); iterator.hasNext();) {

              try {

                    ProcessInstance pi = (ProcessInstance) iterator.next();
                    processInstanceOID = pi.getOID();
                    if (setDataPath) {
                          LOG.info("Inside fetchAndSetProcessDataPaths - Data Paths to set : " + dataPathJson);
                          Set<Map.Entry<String, JsonElement>> jsonMap = dataPathJson.entrySet();
                          for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

                                dataPathId = mapEntry.getKey();
                                datPathValue = mapEntry.getValue().getAsString();

                                getWorkflowService().setOutDataPath(processInstanceOID, dataPathId, datPathValue);

                          }
                    }
                    List<DataPath> dpList = pi.getDescriptorDefinitions();

                    jo = new JsonObject();
                    jo.addProperty("processOID", processInstanceOID);
                    jo.addProperty("processStatus", pi.getState().getName());

                    for (DataPath dp : dpList) {

                          dataPathId = dp.getId();
                          LOG.info("Inside fetchAndSetProcessDataPaths - Fetching Data Paths : " + dataPathId);

                          datPathValue = pi.getDescriptorValue(dataPathId) != null
                                      ? pi.getDescriptorValue(dataPathId).toString() : null;
                          jo.addProperty(dataPathId, datPathValue);

                    }

                    ja.add(jo);
              } catch (ObjectNotFoundException e) {

                    LOG.info("Inside fetchAndSetProcessDataPaths - Cannot find data path : " + dataPathId + " : "
                                + datPathValue + " for process OID : " + processInstanceOID);

              }

        }
        mainObj.add("ProcessDetails", ja);
        return mainObj;
  }

  // A method by Prerna
  public JsonObject abortProcessInstances(ProcessInstances pis, String heirarchy) {
        JsonObject jo = null;
        JsonArray ja = new JsonArray();
        JsonObject mainObj = new JsonObject();
        long processInstanceOID = 0L;

        for (Iterator iterator = pis.iterator(); iterator.hasNext();) {

              try {

                    jo = new JsonObject();
                    ProcessInstance pi = (ProcessInstance) iterator.next();
                    processInstanceOID = pi.getOID();
                    ProcessInstance abortedProcess = null;
                    if (!pi.getState().getName().equals(ProcessInstanceState.Aborted.toString())) {
                          if (heirarchy.equals("subProcess")) {
                                abortedProcess = getWorkflowService().abortProcessInstance(processInstanceOID,
                                            AbortScope.SubHierarchy);
                          } else {

                                abortedProcess = getWorkflowService().abortProcessInstance(processInstanceOID,
                                            AbortScope.RootHierarchy);

                          }
                          jo.addProperty("processOid", abortedProcess.getOID());
                          jo.addProperty("aborted", "true");
                          ja.add(jo);

                    }
              } catch (ObjectNotFoundException e ) {

            	  LOG.info("Inside abortProcessInstances - Cannot abort process for process OID : "
                          + processInstanceOID + "\n" + e.getMessage());
              jo.addProperty("aborted", "false");
              ja.add(jo);

              }

        }
        mainObj.add("ProcessDetails", ja);
        return mainObj;
  }

	
	/*public JsonObject getProcessDataPaths(JsonObject jsonObject) {

        String processId = null;
        boolean setDataPath = false;

        processId = jsonObject.get("processId").getAsString();
        if (jsonObject.get("setDataPath") != null) {
               setDataPath = Boolean.parseBoolean(jsonObject.get("setDataPath").getAsString());
               jsonObject.remove("setDataPath");
        }
        JsonObject mainObj;
        JsonObject dataPathJson = null;

        ProcessInstanceQuery piQuery = ProcessInstanceQuery.findForProcess(processId, false);
        piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);
        jsonObject.remove("processId");
        Set<Map.Entry<String, JsonElement>> jsonMap = jsonObject.entrySet();

        LOG.info("inside getProcessDataPaths  method : Process ID : " + processId);

        for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

               if (!mapEntry.getKey().equals("DataPaths")) {

                     String[] dataElement = mapEntry.getKey().split("\\.");
                     Number dataValue;
                     if (dataElement.length > 1) {

                            if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
                                   dataValue = mapEntry.getValue().getAsJsonPrimitive().getAsNumber();
                                   if (dataValue instanceof Integer || dataValue instanceof Long) {
                                          piQuery.where(DataFilter.isEqual(dataElement[0], dataElement[1],
                                                        mapEntry.getValue().getAsLong()));
                                   } else if (dataValue instanceof Float || dataValue instanceof Double) {
                                          piQuery.where(DataFilter.isEqual(dataElement[0], dataElement[1],
                                                        mapEntry.getValue().getAsDouble()));
                                   }
                            } else {
                                   piQuery.where(
                                                 DataFilter.isEqual(dataElement[0], dataElement[1], mapEntry.getValue().getAsString()));

                            }

                     } else {
                            if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
                                   dataValue = mapEntry.getValue().getAsJsonPrimitive().getAsNumber();
                                   if (dataValue instanceof Integer || dataValue instanceof Long) {
                                          piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsLong()));
                                   } else if (dataValue instanceof Float || dataValue instanceof Double) {
                                          piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsDouble()));
                                   }
                            } else {
                                   piQuery.where(DataFilter.isEqual(dataElement[0], mapEntry.getValue().getAsString()));

                            }

                     }

               } else {
                     if(setDataPath){
                     LOG.info("Data Paths to set : " + mapEntry.getValue().getAsJsonObject());
                     dataPathJson = mapEntry.getValue().getAsJsonObject();
                     }

               }
                            }

        ProcessInstances pis = getQueryService().getAllProcessInstances(piQuery);

        LOG.info("Process Instance fetched  - Count : " + pis.getSize());

        mainObj = fetchAndSetProcessDataPaths(pis, setDataPath, dataPathJson);
        return mainObj;
 }


 private JsonObject fetchAndSetProcessDataPaths(ProcessInstances pis, boolean setDataPath, JsonObject dataPathJson) {
        JsonObject jo = null;
        JsonArray ja = new JsonArray();
        JsonObject mainObj = new JsonObject();
        String dataPathId = null;
        String datPathValue = null;
        long processInstanceOID = 0L;

        
               for (Iterator iterator = pis.iterator(); iterator.hasNext();) {
        
                     try {
                     
                     ProcessInstance pi = (ProcessInstance) iterator.next();
                      processInstanceOID = pi.getOID();
                     if (setDataPath) {
                            LOG.info("Inside fetchAndSetProcessDataPaths - Data Paths to set : " + dataPathJson);
                            Set<Map.Entry<String, JsonElement>> jsonMap = dataPathJson.entrySet();
                            for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

                                   dataPathId = mapEntry.getKey();
                                   datPathValue = mapEntry.getValue().getAsString();

                                   getWorkflowService().setOutDataPath(processInstanceOID, dataPathId, datPathValue);

                            }
                     }
                     List<DataPath> dpList = pi.getDescriptorDefinitions();

                     jo = new JsonObject();
                     jo.addProperty("processOID", processInstanceOID);
                     jo.addProperty("processStatus", pi.getState().getName());

                     for (DataPath dp : dpList) {

                            dataPathId = dp.getId();
                            LOG.info("Inside fetchAndSetProcessDataPaths - Fetching Data Paths : " + dataPathId);

                            datPathValue = pi.getDescriptorValue(dataPathId) != null
                                          ? pi.getDescriptorValue(dataPathId).toString() : null;
                            jo.addProperty(dataPathId, datPathValue);

                     }

                     ja.add(jo);
               }catch (ObjectNotFoundException e) {

                     LOG.info("Inside fetchAndSetProcessDataPaths - Cannot find data path : " + dataPathId + " : " + datPathValue
                                   + " for process OID : " + processInstanceOID);

               }

        }             
               mainObj.add("ProcessDetails", ja);
        return mainObj;
 }*/

 
 
 
 //Experimental - it is being used by rest service to start new business process
 
 public ProcessInstance startProcessById(String processId, Map<String, Object> dataMap) {
     ProcessInstance process = this.getWorkflowService().startProcess(processId, dataMap, true);
     return process;
 }
 
 // Experimental- to attach documents to a different process
 
 public void attachDocuments(long fromProcessOid,long toProcessOid){
	 List<Document> documents = this.getAttachedDocuments(fromProcessOid);
	 this.addDocumentsToProcessAttachments(documents, toProcessOid);
 }
  

}
