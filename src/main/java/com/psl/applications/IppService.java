package com.psl.applications;

import static org.eclipse.stardust.engine.core.spi.dms.RepositoryIdUtils.stripRepositoryId;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URLConnection;
import com.psl.beans.ApplicationConstants;
import javax.ws.rs.core.*;
import javax.media.jai.RenderedImageAdapter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.error.InvalidValueException;
import org.eclipse.stardust.common.error.ObjectNotFoundException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.model.*;
import org.eclipse.stardust.engine.api.query.*;
import org.eclipse.stardust.engine.api.runtime.*;
import org.eclipse.stardust.engine.core.preferences.configurationvariables.ConfigurationVariable;
import org.eclipse.stardust.engine.core.preferences.configurationvariables.ConfigurationVariables;
import org.eclipse.stardust.engine.core.runtime.beans.AbortScope;
import org.eclipse.stardust.engine.core.spi.dms.RepositoryIdUtils;
import com.google.gson.*;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import com.psl.beans.ApplicationConstants;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

public class IppService {

	private ServiceFactory serviceFactory;
	

	private String ippPassword;
	private String ippUser;
	
	static final Logger LOG = LogManager.getLogger(IppService.class);
	private String[] activityIds;
	private String[] processIds;
	private HashSet<String> actIds;

	public IppService() {
	}

	/**
	 * Method to get the query service
	 * 
	 * @return Query Service object
	 */

	public QueryService getQueryService() {
		return getServiceFactory().getQueryService();
	}

	/**
	 * Method to get the document service
	 * 
	 * @return Document Service object
	 */

	public DocumentManagementService getDocumentManagementService() {
		return getServiceFactory().getDocumentManagementService();
	
	}

	/**
	 * Method to get the administration service
	 * 
	 * @return Administration Service object
	 */

	public AdministrationService getAdministrationService() {
		return getServiceFactory().getAdministrationService();

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

	public String[] getActivityIds() {
		return activityIds;
	}

	public void setActivityIds(String[] activityIds) {
		this.activityIds = activityIds;
		this.actIds = new HashSet<String>(Arrays.asList(activityIds));
	}

	public String[] getProcessIds() {
		return processIds;
	}

	public void setProcessIds(String[] processIds) {
		this.processIds = processIds;
	}

	/**
	 * Method to get the service factory
	 * 
	 * @return Service factory object
	 */

	private ServiceFactory getServiceFactory() {
		if (serviceFactory == null) {
			serviceFactory = ServiceFactoryLocator.get(ippUser, ippPassword);
		}
		return serviceFactory;
	}

	/**
	 * Method to get the Workflow service
	 * 
	 * @return Workflow Service object
	 */

	public WorkflowService getWorkflowService() {

		return getServiceFactory().getWorkflowService();

	}

	public UserService getUserService() {

		return getServiceFactory().getUserService();

	}

	/**
	 * Method to get the any data type associated with a process
	 * 
	 * @param long
	 *            process OID
	 * @param String
	 *            Data ID
	 * 
	 * @return object
	 */

	public Object getProcessData(Long processOID, String dataID) {

		try {
			return getWorkflowService().getInDataPath(processOID, dataID);
		} catch (ObjectNotFoundException e) {
			LOG.warn("Could not find Data Path " + dataID + " on scope process Instance " + processOID);
			return null;
		} catch (InvalidValueException e) {
			LOG.warn("Could not get data for Data Path " + dataID + " on scope process Instance " + processOID);
			return null;
		}
	}

	/**
	 * Fetches the activites details of the process based on the processOID
	 * activityIds for Scanning and Indexing process
	 * 
	 * @param processOid
	 * 
	 * @return List<Map<String,String>>
	 */
	public List<Map<String, String>> getActivitiesDetailsForIndexing(long processOid) {

		String activityName = null;
		String modelParticipantName = null;
		String activityState = null;
		String startTime = null;
		String lastModificationTime = null;
		String userName = null;
		List<Map<String, String>> activitiesList = new ArrayList<Map<String, String>>();

		ActivityInstanceQuery activityQuery = ActivityInstanceQuery.findAlive();
		activityQuery.where(new ProcessInstanceFilter(processOid));

		/*
		 * FilterOrTerm orFilter = activityQuery.getFilter().addOrTerm();
		 * orFilter.add(new ProcessInstanceFilter(processOid));
		 */

		/*
		 * for (int x = 0; x < roleNamesArr.length; x++) { grant =
		 * roleNamesArr[x]; PerformingParticipantFilter performerFilter =
		 * PerformingParticipantFilter.forModelParticipant(grant);
		 * orFilter.or(performerFilter); }
		 */

		List<ActivityInstance> activityList = getQueryService().getAllActivityInstances(activityQuery);

		boolean foundWaitActivity = false;
		for (ActivityInstance ai : activityList) {
			if (actIds.contains(ai.getActivity().getId())) {
				Map<String, String> activityDetailsMap = new HashMap<String, String>();
				Activity activity = ai.getActivity();
				activityName = activity.getName();

				ModelParticipant modelParticipant = activity.getDefaultPerformer();
				modelParticipantName = (modelParticipant != null) ? modelParticipant.getName() : null;

				UserInfo userInfo = ai.getPerformedBy();
				userName = (userInfo != null) ? userInfo.getFirstName() + " " + userInfo.getLastName() : null;

				ActivityInstanceState state = ai.getState();
				if (state != null) {
					activityState = state.getName();
				}

				if (ai.getStartTime() != null) {
					startTime = ai.getStartTime().toString();

				}
				if (ai.getLastModificationTime() != null) {
					lastModificationTime = ai.getLastModificationTime().toString();
				}

				activityDetailsMap.put("activityInstanceOid", Long.toString(ai.getOID()));
				activityDetailsMap.put("activityName", activityName);
				activityDetailsMap.put("modelParticipant", modelParticipantName);
				activityDetailsMap.put("performedBy", userName);
				activityDetailsMap.put("state", activityState);
				activityDetailsMap.put("startTime", startTime);
				activityDetailsMap.put("lastModificationTime", lastModificationTime);

				activitiesList.add(activityDetailsMap);
				foundWaitActivity = true;
				break;
			}
			if (foundWaitActivity) {
				break;
			}
		}

		LOG.info("Activity Details :" + activitiesList);
		return activitiesList;
	}

	/**
	 * Fetches the documents attached to process instance, oid passed
	 * 
	 * @param processOid
	 *            OID of the process instance
	 * @return attached documents
	 */
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

	/**
	 * This method returns a list of documents in a paginated way based on size
	 * and page number on the UI.
	 * 
	 * @param paginatedDocsList
	 * @param totalCount
	 * @param originalDocsList
	 * @return
	 */
	public Response preparePaginatedJSONData(List<Document> paginatedDocsList, int totalCount,
			List<Document> originalDocsList) {
		JsonObject mainObj = new JsonObject();
		try {
			mainObj = prerpareDocJSON(paginatedDocsList);
			mainObj.addProperty("totalCount", totalCount);

			String docTypesStr = "";
			String dt = "";
			for(Document doc : originalDocsList) {
				if(doc.getProperty("DocumentType") != null) {
					List<String> docTypes = (List) doc.getProperty("DocumentType");
					
					dt = getCSVS(docTypes); 
					dt = dt != null && dt.equalsIgnoreCase("null") ? "" : dt;
					
					if(dt.isEmpty())
						continue;
					
					if(docTypesStr.isEmpty())
						docTypesStr = StringUtils.join(docTypes.iterator(), ",");
					else
						docTypesStr = docTypesStr + "," + StringUtils.join(docTypes.iterator(), ",");
				}
			} 

			mainObj.addProperty("docTypesList", docTypesStr);
		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception preparePaginatedJSONData  -- " + e);
			LOG.info(
					"GetDocumentListService NEW REST API : Exception preparePaginatedJSONData -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception preparePaginatedJSONData -- " + e.getCause());
			return Response.ok("JSON Error!").build();
		}

		return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE)
				.header("Access-Control-Allow-Origin", "*").build();
	}

	/**
	 * This method forms the JSON object which is used by Document services to
	 * return to the callee
	 * 
	 * @param docsList
	 * 
	 * @return JsonObject
	 */

	public JsonObject prerpareDocJSON(List<Document> docsList) {
		JsonObject jo = null;
		JsonArray ja = new JsonArray();
		JsonObject mainObj = new JsonObject();
		String version = "";
		try {
			for (Document doc : docsList) {
				version = doc.getRevisionName().equalsIgnoreCase("UNVERSIONED") ? "1.0" : doc.getRevisionName();

				jo = new JsonObject();
				jo.addProperty("docName", doc.getName());
				jo.addProperty("View", doc.getId());
				jo.addProperty("createdDate", doc.getDateCreated() == null ? " " : doc.getDateCreated().toString());
				jo.addProperty("version", doc.getRevisionName() == null ? "1.0" : version);
				jo.addProperty("docType", getDocumentTypeString(doc));
				try {
					jo.addProperty("MetaDataDocVersion",
							doc.getProperties().get(ApplicationConstants.META_DATA_MEMBER_NO.getValue()).toString());
				} catch (Exception e) {
					jo.addProperty("MetaDataDocVersion", "memberno");
				}
				try {
					jo.addProperty("customKeywords",
							doc.getProperties().get(ApplicationConstants.META_DATA_REQUEST_TYPE.getValue()).toString());
				} catch (Exception e) {
					jo.addProperty("customKeywords", "");
				}
				ja.add(jo);
			}

			mainObj.add("AvlClaimDocumentsSD", ja);
			return mainObj;
		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData  -- " + e);
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getCause());
			return mainObj;
		}
	}

	/**
	 * Saves the document against the Process OID passed by creating a Document
	 * object
	 * 
	 * @param attachment
	 * @param request
	 * @param processOid
	 * @param docTypes
	 * @param memberNo
	 * @param schemeNo
	 * @param workType
	 * @return
	 */
	public Document saveDocumentinIpp(byte[] documentContent, String filename, String contentType, long processOid,
			Map properties) {
		Document document = this.createDocument(documentContent, filename, contentType, processOid, properties);
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		this.addDocumentsToProcessAttachments(documents, processOid);
		return document;
	}

	public Document createDocument(byte[] documentContent, String filename, String contentType, long processOid,
			Map<String, Object> properties) {

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
		// Check if the exception is caused by a "file exists exception"

		Document document2 = getDocumentManagementService().getDocument(defaultPath + "/" + filename);
		Document document1;
		if (document2 != null) {
			defaultPath = defaultPath + new Date().getTime();
			Folder folder = DmsUtils.ensureFolderHierarchyExists(defaultPath, getDocumentManagementService());
			try {
				document1 = getDocumentManagementService().createDocument(defaultPath, documentInfo, documentContent,
						null);

			} catch (DocumentManagementServiceException e1) {
				LOG.warn("Document Management service exception: " + e);
				throw e;
			}
		} else {
			throw e;
		}
		return document1;
	}

	/**
	 * Updates the new document list as the PROCESS_ATTACHMENT Data paths
	 * 
	 * @param docList
	 * @param processOid
	 * @return
	 */
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

	/**
	 * Method to set the any data type associated with a process
	 * 
	 * @param long
	 *            process OID
	 * @param String
	 *            Data ID
	 * @param Object
	 *            data
	 * 
	 * @return object
	 */

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
		JsonObject mainObj = new JsonObject();
		try {
			mainObj = prerpareDocJSON(docsList);
		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData  -- " + e);
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception prepareJSONData -- " + e.getCause());
			return Response.ok("JSON Error!").build();
		}

		return Response.ok(mainObj.toString(), MediaType.APPLICATION_JSON_TYPE)
				.header("Access-Control-Allow-Origin", "*").build();
	}

	public String getDocumentTypeString(Document doc) {

		Map<String, Object> props = doc.getProperties();
		LOG.info("GetDocList Rest Service - Document Types properties - " + props);
		List<String> docTypes = new ArrayList<String>();
		if (props != null && props.size() > 0) {
			docTypes = (List<String>) props.get("DocumentType");
		}
		String dt = getCSVS(docTypes); ;
		dt = dt != null && dt.equalsIgnoreCase("null") ? "" : dt;
		return dt;
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

	/**
	 * Fetches the content of the document based on the JCR ID passed
	 * 
	 * @param String
	 *            documentID JCR ID
	 * @return String Stringified JSON which consists of File Content (File Name
	 *         and Byte Content)
	 */
	public String getDocumentContent(String documentID) {

		byte[] docContent = null;
		String fileName = null;
		Document doc = null;
		try {
			doc = getDocumentManagementService().getDocument(documentID);

			docContent = getDocumentManagementService().retrieveDocumentContent(documentID);

			LOG.info(doc.getRepositoryId());
			LOG.info(doc.getSize());
			LOG.info(doc.getContentType());
			LOG.info(documentID);
			fileName = doc.getName();
		} catch (Exception e) {
			LOG.info("Error in retrieving document content with document ID " + documentID);
			LOG.info("Error stack trace is" + e.getStackTrace());
			return "{\"fileName\":" + null + ",\n \"documentContent\":" + null + ",\n \"error\":"
					+ "\"No Document Found with the requested DOC ID !!\"" + "}";

		}
		int[] arr = new int[docContent.length];
		int i = 0;
		for (byte b : docContent) {
			arr[i++] = (0x000000FF) & b;

		}

		String response = "{\"fileName\":\"" + fileName + "\",\n \"documentContent\":" + Arrays.toString(arr)
				+ ",\n \"error\":" + null + "}";
		LOG.debug("The Doc Content is :" + Arrays.toString(arr));

		return response;
	}

	/**
	 * Fetches the activites details of the process based on the OID. Can be
	 * filtered to fetch details of only the interactive activities
	 * 
	 * @param processOid
	 * @param onlyInteractive
	 * 
	 * @return List<Map<String,String>>
	 */
	public List<Map<String, String>> getActivitiesDetails(long processOid, boolean onlyInteractive) {

		String activityName = null;
		String modelParticipantName = null;
		String activityState = null;
		String startTime = null;
		String lastModificationTime = null;
		String userName = null;
		List<Map<String, String>> activitiesList = new ArrayList<Map<String, String>>();

		ActivityInstanceQuery activityQuery = ActivityInstanceQuery.findForProcessInstance(processOid);
		activityQuery.where(new ProcessInstanceFilter(processOid));
		List<ActivityInstance> activityList = getQueryService().getAllActivityInstances(activityQuery);

		for (ActivityInstance ai : activityList) {

			Map<String, String> activityDetailsMap = new HashMap<String, String>();

			Activity activity = ai.getActivity();

			if (activity != null && (onlyInteractive ? activity.isInteractive() : true)) {
				ImplementationType type = activity.getImplementationType();
				if (type != null) {

					if (type.getName().equalsIgnoreCase(ImplementationType.Application.getName())
							|| type.isSubProcess()) {

						activityName = activity.getName();

						ModelParticipant modelParticipant = activity.getDefaultPerformer();
						modelParticipantName = (modelParticipant != null) ? modelParticipant.getName() : null;

						UserInfo userInfo = ai.getPerformedBy();
						userName = (userInfo != null) ? userInfo.getFirstName() + " " + userInfo.getLastName() : null;

						ActivityInstanceState state = ai.getState();
						if (state != null) {
							activityState = state.getName();
						}

						if (ai.getStartTime() != null) {
							startTime = ai.getStartTime().toString();

						}
						if (ai.getLastModificationTime() != null) {
							lastModificationTime = ai.getLastModificationTime().toString();
						}

						activityDetailsMap.put("activityInstanceOid", Long.toString(ai.getOID()));
						activityDetailsMap.put("activityName", activityName);
						activityDetailsMap.put("modelParticipant", modelParticipantName);
						activityDetailsMap.put("performedBy", userName);
						activityDetailsMap.put("state", activityState);
						activityDetailsMap.put("startTime", startTime);
						activityDetailsMap.put("lastModificationTime", lastModificationTime);

						activitiesList.add(activityDetailsMap);
					}
				}
			}

		}

		LOG.info("Process Details :" + activitiesList);
		return activitiesList;
	}

	/**
	 * Fetches the process details of the OID passed
	 * 
	 * @param long
	 *            processOid
	 * @return Map<String,String>
	 */
	public Map<String, String> getProcessDetails(long processOid) {

		Map<String, String> processDetails = new HashMap<String, String>();

		ProcessInstance processInstance = getWorkflowService().getProcessInstance(processOid);
		if (processInstance != null) {

			processDetails.put("processName", processInstance.getProcessName());
			processDetails.put("rootProcessOid", Long.toString(processInstance.getRootProcessInstanceOID()));
			LOG.info("Process Details :" + processDetails);
			ProcessInstanceState processState = processInstance.getState();
			String processStateName = (processState != null) ? processState.getName() : null;
			processDetails.put("processState", processStateName);
			LOG.info("Process Details :" + processDetails);

			User user = processInstance.getStartingUser();
			String userName = (user != null) ? user.getName() : null;
			processDetails.put("startingUser", userName);
			LOG.info("Process Details :" + processDetails);

			processDetails.put("startTime", processInstance.getStartTime().toString());
			LOG.info("Process Details :" + processDetails);
			if (processState != null && !(processState.getValue() == ProcessInstanceState.ACTIVE
					|| processState.getValue() == ProcessInstanceState.INTERRUPTED)) {
				processDetails.put("endTime", processInstance.getTerminationTime().toString());
			}
			LOG.info("Process Details :" + processDetails);
		}

		return processDetails;
	}

	/**
	 * Fetches processes based on the data filters (Data ID) for Scanning and
	 * Indexing process search
	 * 
	 * @param jsonObject
	 * @return ProcessInstances
	 */
	public ProcessInstances fetchProcessInstancesForIds(JsonObject jsonObject) {
		ProcessInstances pis = null;
		String completedProcesses = "";
		completedProcesses = jsonObject.get("showCompleted") != null && !jsonObject.get("showCompleted").isJsonNull()
				? jsonObject.get("showCompleted").getAsString() : "";
		ProcessInstanceQuery piQuery = null;

		try {
			if (completedProcesses.equalsIgnoreCase("Yes")) {
				ProcessInstanceState[] states = { ProcessInstanceState.Aborted, ProcessInstanceState.Completed };
				piQuery = ProcessInstanceQuery.findInState(states);
			} else{
				piQuery = ProcessInstanceQuery.findAlive();
			} 
			
			FilterOrTerm processIdOrTerm = piQuery.getFilter().addOrTerm();
			for (String processId : processIds) {
				processIdOrTerm.add(new ProcessDefinitionFilter(processId, false));
			}

			// piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);
			Set<Map.Entry<String, JsonElement>> jsonMap = jsonObject.entrySet();

			for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

				String[] dataElement = mapEntry.getKey().split("\\.");
				Number dataValue;
				if (dataElement.length > 1) {
					if (dataElement[0].equals("IndexData") && dataElement[1].equals("WorkType")) {
						continue;
					} else if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
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
					if (dataElement[0].equals("showCompleted")) {
						continue;
					} else if (mapEntry.getValue().getAsJsonPrimitive().isNumber()) {
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

			pis = getQueryService().getAllProcessInstances(piQuery);
			LOG.info("Process Instance fetched  - Count : " + pis.getSize());
		} catch (Exception e) {
			LOG.info("Exception inside fetchProcessInstancesForIds while fetching process details 1" + e.getMessage());
			LOG.info("Exception inside fetchProcessInstancesForIds while fetching process details 2" + e.getCause());
			LOG.info("Exception inside fetchProcessInstancesForIds while fetching process details 3"
					+ e.fillInStackTrace());
		}
		return pis;
	}

	/**
	 * Parses the received string and forms the JSON Object
	 * 
	 * @param String
	 *            input
	 * @return JsonObject
	 */

	public JsonObject parseJSONObject(String input) {
		JsonObject jsonObject = null;
		JsonParser jp = new JsonParser();
		JsonElement parsedData = jp.parse(input);
		if (parsedData != null && parsedData.isJsonObject()) {
			jsonObject = parsedData.getAsJsonObject();
		}
		return jsonObject;
	}

	/**
	 * Updates the existing document with the updated properties
	 * 
	 * @param String
	 *            docId
	 * @param long
	 *            processOid
	 * @param Map
	 *            properties
	 * 
	 * @return Document
	 */
	public Document updateNonSecureDocument(String docId, long processOid, Map properties) {

		Document document = getDocumentManagementService().getDocument(docId);

		document.getProperties().putAll(properties);

		LOG.info("updateNonsecureDocument: " + docId + " processOid: " + processOid + " document path: "
				+ document.getPath() + "Content- type :" + document.getContentType() + "Properties : "
				+ document.getProperties());
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

		LOG.info("Updated Document properties  : " + document.getProperties());
		Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
		List<Document> attachments = null;
		if (processData == null) {
			LOG.info("Process Attachments empty");
			attachments = new ArrayList<Document>();

		} else {
			LOG.info("Process Attachments found: " + processData.getClass() + " attachmenmts details :"
					+ (List) processData);
			attachments = (List) processData;
		}

		Iterator<Document> itr = attachments.iterator();
		while (itr.hasNext()) {
			Document doc = itr.next();
			if (stripRepositoryId(doc.getId()).equalsIgnoreCase(stripRepositoryId(document.getId()))) {
				// remove old document
				LOG.info("Removind old document , document details are : description -> " + doc.getDescription()
						+ " properties : " + doc.getProperties());
				itr.remove();
				break;
			}
		}

		if (processData != null) {
			LOG.info("After removing : " + processData.getClass() + " attachmenmts details :" + (List) processData);
			attachments.add(document);
			LOG.info("After adding : " + processData.getClass() + " attachmenmts details :" + (List) processData);
		}
		LOG.info("Added document with new document type as : document types " + document.getDocumentType());
		LOG.info("Added Document properties  : " + document.getProperties());
	
		LOG.info("Setting process attachments : List of attachments " + attachments);

		setProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), attachments);
		return document;
	}

	/**
	 * Populates amd forms the map of the document properties
	 * 
	 * @param String
	 *            schemeNo
	 * @param String
	 *            memberNo
	 * @param String
	 *            worktype
	 * @param String
	 *            doctypes
	 * 
	 * @return Map<String,Object> This is a properties map
	 */
	public Map<String, Object> populateDocumentProperties(String schemeNo, String memberNo, String workType,
			String docTypes) {
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

	/**
	 * Delete the document based on the JCR ID passed(Not used)
	 * 
	 * @param String
	 *            docId JCR ID
	 * @return
	 */
	public void deleteDocuments(String docId) {
		LOG.info("Inside IPP Service. Delete Document method. Document Id -: " + docId);
		getDocumentManagementService().removeDocument(docId);
	}

	/**
	 * Removes document from the process attachments as well as from the JCR
	 * based on the JCR ID and Process ID passed
	 * 
	 * @param String
	 *            DocID
	 * @param long
	 *            processOID Process OID and JCR ID of the document to be
	 *            removed
	 * @return
	 */
	public void removeDocument(String docId, long processOid) {

		List<Document> processAttachments = null;
		List<Document> attachedDocs = new ArrayList<Document>();
		Object processData = getProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue());

		processAttachments = (List<Document>) processData;
		attachedDocs.addAll(processAttachments);

		for (Document doc : attachedDocs) {
			if (doc.getId().equalsIgnoreCase(docId)) {
				LOG.info("Document : docId : " + doc.getId());
				processAttachments.remove(doc);
				getDocumentManagementService().removeDocument(docId);
			}
		}
		LOG.info("Size of process attachment :" + processAttachments.size());

		setProcessData(processOid, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), processAttachments);
	}

	/**
	 * Fetches processes based on the data filters (Data ID)
	 * 
	 * @param jsonObject
	 * @return ProcessInstances
	 */

	public ProcessInstances fetchProcessInstances(JsonObject jsonObject) {

		String processId = null;
		if (jsonObject.get("processId") != null) {
			processId = jsonObject.get("processId").getAsString();
		}

		ProcessInstanceQuery piQuery = null;

		if (processId != null && !processId.isEmpty()) {
			piQuery = ProcessInstanceQuery.findForProcess(processId, false);
		} else {
			piQuery = ProcessInstanceQuery.findAlive();
		}
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
	 * Fetches the data paths of Active,Completed,Created process instances.
	 * Also,sets the data paths based on the flag passed
	 * 
	 * @param pis
	 * @param setDataPath
	 * @param dataPathJson
	 * @return JsonObject
	 */
	public JsonObject fetchAndSetProcessDataPaths(ProcessInstances pis, boolean setDataPath, JsonObject dataPathJson) {
		JsonObject jo = null;
		JsonArray ja = new JsonArray();
		JsonObject mainObj = new JsonObject();
		String dataPathId = null;
		String datPathValue = null;
		long processInstanceOID = 0L;
		ProcessInstanceState processState = null;

		for (Iterator iterator = pis.iterator(); iterator.hasNext();) {

			try {

				ProcessInstance pi = (ProcessInstance) iterator.next();
				processInstanceOID = pi.getOID();
				processState = pi.getState();
				LOG.info("Inside fetchAndSetProcessDataPaths - Process OID : " + processInstanceOID + "Process State : "
						+ processState);
				if (processState != ProcessInstanceState.Aborted && processState != ProcessInstanceState.Aborting
						&& processState != ProcessInstanceState.Interrupted) {
					if (setDataPath) {
						LOG.info("Inside fetchAndSetProcessDataPaths - Data Paths to set : " + dataPathJson);
						Set<Map.Entry<String, JsonElement>> jsonMap = dataPathJson.entrySet();

						if (jsonMap != null) {
							for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {

								dataPathId = mapEntry.getKey();
								datPathValue = mapEntry.getValue().getAsString();

								getWorkflowService().setOutDataPath(processInstanceOID, dataPathId, datPathValue);

							}
						}
					}
					Map<String, Serializable> dataPathMap = getWorkflowService().getInDataPaths(processInstanceOID,
							null);
					jo = new JsonObject();
					jo.addProperty("processOID", processInstanceOID);
					jo.addProperty("processStatus", pi.getState().getName());
					if (dataPathMap != null) {
						LOG.info("Inside fetchAndSetProcessDataPaths - Data Paths to set : " + dataPathJson);
						Set<Map.Entry<String, Serializable>> jsonMap = dataPathMap.entrySet();
						if (jsonMap != null) {
							for (Map.Entry<String, Serializable> mapEntry : jsonMap) {
								dataPathId = mapEntry.getKey();

								LOG.info("Inside fetchAndSetProcessDataPaths - Fetching Data Paths : " + dataPathId);

								datPathValue = mapEntry.getValue() + "";
								jo.addProperty(dataPathId, datPathValue);

							}
						}
					}
					ja.add(jo);
				}
			} catch (ObjectNotFoundException e) {

				LOG.info("Inside fetchAndSetProcessDataPaths - Cannot find data path : " + dataPathId + " : "
						+ datPathValue + " for process OID : " + processInstanceOID);

			}

		}
		mainObj.add("ProcessDetails", ja);
		return mainObj;
	}

	/**
	 * Aborts the process instances based on the hierarchy option passed
	 * 
	 * @param pis
	 *            Process Instances to be aborted
	 * @param heirarchy
	 *            hierarchy of the process abortion
	 * @return
	 */
	public JsonObject abortProcessInstances(ProcessInstances pis, String heirarchy) {
		JsonObject jo = null;
		JsonArray ja = new JsonArray();
		JsonObject mainObj = new JsonObject();
		long processInstanceOID = 0L;
		String processState = null;

		if (pis != null && pis.getSize() > 0) {
			for (Iterator iterator = pis.iterator(); iterator.hasNext();) {
				LOG.info("Inside abortProcessInstances - " + processInstanceOID);
				ProcessInstance pi = (ProcessInstance) iterator.next();
				processInstanceOID = pi.getOID();
				processState = pi.getState().getName();
				jo = abortProcess(heirarchy, processInstanceOID, processState);
				ja.add(jo);
			}
		}
		mainObj.add("ProcessDetails", ja);
		return mainObj;
	}

	/**
	 * 
	 * @param heirarchy
	 *            process hierarchy of the process to be aborted
	 * @param processInstanceOID
	 *            Process OID of the process
	 * @param processState
	 *            Current State of the process instance
	 * @return
	 * @throws ObjectNotFoundException
	 */
	public JsonObject abortProcess(String heirarchy, long processInstanceOID, String processState) {
		JsonObject jo = new JsonObject();
		LOG.info("Inside abortProcessInstances - " + processInstanceOID + heirarchy);
		ProcessInstance abortedProcess = null;
		jo.addProperty("processOid", processInstanceOID);
		try {
			if (!(processState.equals(ProcessInstanceState.Aborted.toString()))
					&& !(processState.equals(ProcessInstanceState.Completed.toString()))
					&& !(processState.equals(ProcessInstanceState.Aborting.toString()))) {
				if (heirarchy.equals("subProcess")) {
					abortedProcess = getWorkflowService().abortProcessInstance(processInstanceOID,
							AbortScope.SubHierarchy);
				} else {
					abortedProcess = getWorkflowService().abortProcessInstance(processInstanceOID,
							AbortScope.RootHierarchy);
				}
				jo.addProperty("aborted", "true");
			} else {
				jo.addProperty("aborted", "false");
			}
		} catch (Exception e) {
			jo.addProperty("aborted", "false");
			LOG.info("Cannot Abort Process for Process OID : " + processInstanceOID);
			LOG.info("Cannot Abort Process for Process OID : " + e.getCause());
			LOG.info("Cannot Abort Process for Process OID : " + e.getMessage());
			LOG.info("Cannot Abort Process for Process OID : " + e.getStackTrace());
		}
		return jo;
	}

	/**
	 * (Not Used)Experimental - it is being used by rest service to start new
	 * business
	 * 
	 * @param String
	 *            ProcessId pis
	 * @param Map<String,Object>
	 *            dataMap
	 * @return ProcessInstance
	 */
	public ProcessInstance startProcessById(String processId, Map<String, Object> dataMap) {
		return this.getWorkflowService().startProcess(processId, dataMap, true);
	}

	/**
	 * (Not Used) Used to attach documents from one process instance to another
	 * process instances
	 * 
	 * @param long
	 *            fromProcessOid
	 * @param long
	 *            toProcessOid
	 * @return
	 */

	public void attachDocuments(long fromProcessOid, long toProcessOid) {
		List<Document> documents = this.getAttachedDocuments(fromProcessOid);
		this.addDocumentsToProcessAttachments(documents, toProcessOid);
	}

	/**
	 * Upload documents in a Folder in JCR
	 * 
	 * @param attachment
	 * @param request
	 * @param docTypes
	 * @param memberNo
	 * @param schemeNo
	 * @param workType
	 * @return
	 */
	public Document uploadDocinFolder(byte[] documentContent, String filename, String contentType,
			Map<String, Object> properties) {

		String memberNumber, schemeNumber;

		long dateTime = new Date().getTime();

		schemeNumber = properties.get(ApplicationConstants.META_DATA_SCHEME_NO.getValue()).toString();
		memberNumber = properties.get(ApplicationConstants.META_DATA_MEMBER_NO.getValue()).toString();

		String folderPath = "/Claims_Test/" + schemeNumber + "/" + memberNumber + "/" + dateTime;

		Folder schemeFolderPath = DmsUtils.ensureFolderHierarchyExists(folderPath, getDocumentManagementService());

		LOG.info(schemeFolderPath.getId() + "-----------------------------" + schemeFolderPath.getPath());

		DocumentInfo docInfo = DmsUtils.createDocumentInfo(filename);

		Document doc = getDocumentManagementService().createDocument(schemeFolderPath.getId(), docInfo, documentContent,
				null);

		LOG.info(doc.getId());
		return doc;
	}

	/**
	 * Fetches the interrupted process/activity details in an excel format
	 * 
	 * @return
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public File getProcessCount() throws IOException, InvalidFormatException {

		List<HashMap<String, String>> processDetails = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> processDetail;

		ProcessInstanceQuery processQuery = ProcessInstanceQuery.findInState(ProcessInstanceState.Interrupted);
		processQuery.setPolicy(new SubsetPolicy(10000));
		List<ProcessInstance> processInstances = getQueryService().getAllProcessInstances(processQuery);

		String activityState = null;
		String startTime = null;
		String lastModificationTime = null;
		String userName = null;

		Workbook workbook = new XSSFWorkbook();
		CreationHelper createHelper = workbook.getCreationHelper();
		Sheet sheet = workbook.createSheet("Activities");
		Font headerFont = workbook.createFont();
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(IndexedColors.RED.getIndex());
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		Row headerRow = sheet.createRow(0);
		String[] columns = { "processName", "rootProcessOid", "processState", "startingUser", "startTime",
				"activityInstanceOid", "activityName", "Error Event", "state", "startTime", "lastModificationTime" };
		for (int i = 0; i < columns.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(columns[i]);
			cell.setCellStyle(headerCellStyle);
		}
		CellStyle dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

		for (ProcessInstance processInstance : processInstances) {
			long processOid = processInstance.getRootProcessInstanceOID();
			processDetail = new HashMap<String, String>();
			processDetail.put("processName", processInstance.getProcessName());
			processDetail.put("rootProcessOid", Long.toString(processOid).toString());
			LOG.info("Process Details :" + processDetail);

			ProcessInstanceState processState = processInstance.getState();
			String processStateName = (processState != null) ? processState.getName() : null;
			processDetail.put("processState", processStateName);
			LOG.info("Process Details :" + processDetail);

			User user = processInstance.getStartingUser();
			userName = (user != null) ? user.getName() : null;
			processDetail.put("startingUser", userName);
			LOG.info("Process Details :" + processDetail);

			processDetail.put("startTime", processInstance.getStartTime().toString());
			LOG.info("Process Details :" + processDetail);
			LOG.info("Process Details :" + processDetail);

			ActivityInstanceQuery activityQuery = ActivityInstanceQuery.findForProcessInstance(processOid);
			activityQuery = activityQuery.findInState(ActivityInstanceState.Interrupted);
			activityQuery.where(new ProcessInstanceFilter(processOid));

			activityQuery.setPolicy(new HistoricalEventPolicy(HistoricalEventType.EXCEPTION));

			List<ActivityInstance> activityList = getQueryService().getAllActivityInstances(activityQuery);
			for (ActivityInstance ai : activityList) {
				ActivityInstanceState state = ai.getState();
				if (state != null && !state.getName().equalsIgnoreCase("Interrupted")) {
					continue;
				} else if (state != null) {
					activityState = state.getName();
				}

				if (ai.getStartTime() != null) {
					startTime = ai.getStartTime().toString();

				}
				if (ai.getLastModificationTime() != null) {
					lastModificationTime = ai.getLastModificationTime().toString();
				}

				Activity activity = ai.getActivity();

				List<HistoricalEvent> aiHistory = ai.getHistoricalEvents();

				String historyEventData = "";
				for (int i = 0; i < aiHistory.size(); i++) {
					HistoricalEvent he = aiHistory.get(i);
					// System.out.println(he.getDetails());
					// System.out.println(he.getEventType());
					// System.out.println("---------------------------------");
					// jo.addProperty("Event " + i + " " +
					// he.getEventType().getName(), he.getDetails().toString());
					historyEventData += " " + he.getEventType().getName() + "  " + he.getDetails().toString();
				}

				processDetail.put("Event", historyEventData);

				/*
				 * jo.addProperty("activityInstanceOid", new
				 * Long(ai.getOID()).toString()); jo.addProperty("activityName",
				 * activity.getName()); jo.addProperty("state", activityState);
				 * jo.addProperty("startTime", startTime);
				 * jo.addProperty("lastModificationTime", lastModificationTime);
				 */

				processDetail.put("activityInstanceOid", Long.toString(ai.getOID()).toString());
				processDetail.put("activityName", activity.getName());
				processDetail.put("state", activityState);
				processDetail.put("startTime", startTime);
				processDetail.put("lastModificationTime", lastModificationTime);

				break;
			}

			processDetails.add(processDetail);
		}

		// Create Other rows and cells with data
		int rowNum = 1;
		for (HashMap<String, String> process : processDetails) {
			Row row = sheet.createRow(rowNum++);

			row.createCell(0).setCellValue(process.get("processName"));
			row.createCell(1).setCellValue(process.get("rootProcessOid"));
			row.createCell(2).setCellValue(process.get("processState"));
			row.createCell(3).setCellValue(process.get("startingUser"));
			Cell dateCell = row.createCell(4);
			dateCell.setCellValue(process.get("startTime"));
			dateCell.setCellStyle(dateCellStyle);
			row.createCell(5).setCellValue(process.get("activityInstanceOid"));
			row.createCell(6).setCellValue(process.get("activityName"));
			row.createCell(7).setCellValue(process.get("Event"));
			row.createCell(8).setCellValue(process.get("state"));
			row.createCell(9).setCellValue(process.get("startTime"));
			row.createCell(10).setCellValue(process.get("lastModificationTime"));
		}

		
		File file = new File("Interrupted-Activities.xlsx");
		FileOutputStream fileOut = new FileOutputStream(file);
		workbook.write(fileOut);

		fileOut.close();
		return file;
	
	}

	/**
	 * Deprecated
	 * 
	 * Converts the TIFF documents attached to the process to PDF The below
	 * method doesn't cater for multi strip tiff documents, Please check the
	 * 'convertMultiStripTifToPDF' method which will cater for single as well as
	 * multi strip tiffs
	 * 
	 * @param processOid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void convertDocumentTIFtoPDF(Long processOid) {
		LOG.info("Inside ConvertDocumentMethod");

		List<Document> processAttachments = (List<Document>) getProcessData(processOid,
				ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
		// Checking if there are any process attachments or not
		if (processAttachments != null && !processAttachments.isEmpty()) {
			LOG.info("Received Process Documents #:" + processAttachments.size());
			for (Document doc : processAttachments) {
				try {

					// check for the tiff type of documents
					if (!(doc.getName().toLowerCase().endsWith(".tif")
							|| doc.getName().toLowerCase().endsWith(".tiff")))
						continue;

					LOG.info("Document :" + doc.getName() + " Path: " + doc.getPath() + " JCR UID: " + doc.getId());
					Map<String, Object> properties = doc.getProperties();
					LOG.info("Document Properties #:" + properties);

					// Get the content of the document in bytes, which will be
					// used to update the existing document
					byte[] docContent = getDocumentManagementService().retrieveDocumentContent(doc.getId());

					// creating an output stream to write the converted bytes
					ByteArrayOutputStream outfile = new ByteArrayOutputStream();
					// com.itextpdf.text.Document document = new
					// com.itextpdf.text.Document(PageSize.LETTER.rotate());
					com.itextpdf.text.Document document = new com.itextpdf.text.Document();
					PdfWriter writer = PdfWriter.getInstance(document, outfile);
					RandomAccessFileOrArray myTiffFile = new RandomAccessFileOrArray(docContent);

					// No of pages to iterate on
					int numberOfPages = TiffImage.getNumberOfPages(myTiffFile);
					
					writer.setStrictImageSequence(true);
					document.open();
					
					LOG.info("Coverting Doc from TIF to PDF with JCR UID: " + doc.getId());
					for (int i = 1; i <= numberOfPages; i++) {
						Image tempImage = TiffImage.getTiffImage(myTiffFile, i);

						// Below helps in covering the whole image in the PDF
						// removing unnecessary margins
						tempImage.setAbsolutePosition(0, 0);

						// The below sequence matters, and helps in defining the
						// size of the new page
						// with the existing image page
						Rectangle pageSize = new Rectangle(tempImage.getWidth(), tempImage.getHeight());
						document.setPageSize(pageSize);
						document.newPage();
						document.add(tempImage);
					}
						document.close();
						outfile.flush();

					// Converting the extension to PDF
					String name = doc.getName();
					name = name.substring(0, name.lastIndexOf('.'));
					LOG.info("Doc Full Name:" + doc.getName());
					LOG.info("Doc Name:" + name);
					doc.setName(name + ".pdf");
					LOG.info("Coverted Doc Name:" + doc.getName());

					// Updating the whole document with the new document
					// content,document version and document name
					getDocumentManagementService().updateDocument(doc, outfile.toByteArray(), "", true, "", "", false);

				} catch (Exception e) {
					// JCR will generate the exception when name(like, ABC.pdf
					// is alreay present) or content are same for the document
					LOG.info("Convert Document  : Exception convertDocuments -- " + e);
					LOG.info("Convert Document  : Exception convertDocuments -- " + e.getStackTrace());
					LOG.info("Convert Document  : Exception convertDocuments -- " + e.getCause());

					LOG.info("Skipping document conversion due to the above errors!!");
				}

				// continue for the next document
			}

		} else
			LOG.info("No Documents to Convert");
	}

	/**
	 * Deprecated
	 * 
	 * Convert the tiff doc content to pdf content and return the converted byte
	 * array The below method doesn't cater for multi strip tiff documents,
	 * Please check the 'convertMultiStripTifToPDF' method which will cater for
	 * single as well as multi strip tiffs
	 * 
	 * @param tiffContent
	 * @return byte[]
	 */
	public static byte[] convertTifAttachmentToPDF(byte[] tiffContent) {
		LOG.info("Inside Attachment Convert Method--conversion started");

		// Checking if the content is null
		if (tiffContent != null) {

			try {

				// creating an output stream to write the converted bytes
				ByteArrayOutputStream outfile = new ByteArrayOutputStream();
				// com.itextpdf.text.Document document = new
				// com.itextpdf.text.Document(PageSize.LETTER.rotate());
				com.itextpdf.text.Document document = new com.itextpdf.text.Document();
				PdfWriter writer = PdfWriter.getInstance(document, outfile);
				RandomAccessFileOrArray myTiffFile = new RandomAccessFileOrArray(tiffContent);

				// No of pages to iterate on
				int numberOfPages = TiffImage.getNumberOfPages(myTiffFile);

				writer.setStrictImageSequence(true);
				document.open();

				for (int i = 1; i <= numberOfPages; i++) {
					Image tempImage = TiffImage.getTiffImage(myTiffFile, i);

					// Below helps in covering the whole image in the PDF
					// removing unnecessary margins
					tempImage.setAbsolutePosition(0, 0);

					// The below sequence matters, and helps in defining the
					// size of the new page
					// with the existing image page
					Rectangle pageSize = new Rectangle(tempImage.getWidth(), tempImage.getHeight());
					document.setPageSize(pageSize);
					document.newPage();
					document.add(tempImage);

				}
				document.close();
				outfile.flush();

				LOG.info("Inside Attachment Convert Method--conversion ended");

				return outfile.toByteArray();

			} catch (Exception e) {
				// JCR will generate the exception when name(like, ABC.pdf
				// is alreay present) or content are same for the document
				LOG.info("Convert Document  : Exception convertDocuments -- " + e);
				LOG.info("Convert Document  : Exception convertDocuments -- " + e.getStackTrace());
				LOG.info("Convert Document  : Exception convertDocuments -- " + e.getCause());

				LOG.info("Skipping document conversion due to the above errors!!");
			}

		} else
			LOG.info("No Documents to Convert");

		return tiffContent;
	}

	/**
	 * Deploys the model from GIT Repository or Local File/Folder Location based
	 * on the input flag passed
	 * 
	 * @param input
	 * @return
	 */
	public List<DeploymentInfo> changeConfigVariablesandDeployModel(HashMap<String, String> modelDataList,
			JsonObject jsonObject, boolean gitRepo) {
		LOG.info(" Changing configuration variables and Deploying the Model : Started");
		List<DeploymentInfo> deploymentInfo = new ArrayList<DeploymentInfo>();
		List<DeploymentElement> list = new ArrayList<DeploymentElement>();

		String modelName = "";
		byte[] byteArray = null;
		String server = "";
		String elementName = "";
		Properties server_prop = null;
		Properties config_prop = null;
		List<ConfigurationVariable> cv ;
		ConfigurationVariables cvs ;
		String configName;
		String configValueProp;
		String configValue;
		String serverConfigKey;
		String serverConfigValue;
		String[] serverConfigArr;
		DeploymentElement de;
		Map<String, String> configTypeMap = new HashMap<String, String>();
		String c_key;
		String c_value;


		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			elementName = entry.getKey();
			if (elementName.equalsIgnoreCase("server")) {
				server = entry.getValue().getAsString().toUpperCase();
			}
		}
		server_prop = readPropertiesFile("server-details-map.properties");
		config_prop = readPropertiesFile("configvariable-map.properties");

		String configType = server_prop.getProperty(server);
		String[] configTypeArr = configType.split(",");
		List<String> configTypeList = Arrays.asList(configTypeArr);

		for (String cType : configTypeList) {
			c_key = server + "." + cType;
			c_value = server_prop.getProperty(c_key.trim());
			configTypeMap.put(c_key, c_value);
		}

		for (Map.Entry<String, String> modelData : modelDataList.entrySet()) {
			if ((modelData.getKey() != null || modelData.getKey() != "") && (modelData.getValue() != null || modelData.getValue() != "")) {
				modelName = modelData.getKey();
				if (gitRepo) {
					byteArray = modelData.getValue().getBytes();
				} else {
					byteArray = getBytesFromModelFile(modelData.getValue());
				}
				if(byteArray != null){
					cv = new ArrayList<ConfigurationVariable>();
					cvs = getAdministrationService().getConfigurationVariables(byteArray);
					cv = cvs.getConfigurationVariables();

					for (ConfigurationVariable configurationVariable : cv) {
						configName = configurationVariable.getName();
						configValueProp = config_prop.getProperty(modelName + "." + configName);
						configValue = null;
						serverConfigKey = "";
						serverConfigValue = "";
						if (configValueProp != null) {
							serverConfigArr = configValueProp.split("\\*");

							if (serverConfigArr.length > 1) {
								serverConfigKey = serverConfigArr[0];
								serverConfigKey = serverConfigKey.replace("Server", server);
								serverConfigValue = configTypeMap.get(serverConfigKey);
								configValue = serverConfigValue + serverConfigArr[1];
							} else {
								if (serverConfigArr[0].contains(".")) {
									serverConfigKey = serverConfigArr[0];
									serverConfigKey = serverConfigKey.replace("Server", server);
									serverConfigValue = configTypeMap.get(serverConfigKey);
									configValue = serverConfigValue;
								} else {
									configValue = configValueProp;
								}
							}
							LOG.info("Updating the ConfigValue to = "+configValue);
							configurationVariable.setValue(configValue);
						}

					}

					cvs.setConfigurationVariables(cv);
					getAdministrationService().saveConfigurationVariables(cvs, true);

					de = new DeploymentElement(byteArray);
					list.add(de);
				}

			} else {
				return null;
			}
		}
		DeploymentOptions options = new DeploymentOptions();
		options.setIgnoreWarnings(true);
		deploymentInfo = getAdministrationService().deployModel(list, options);
		return deploymentInfo;
	}

	
	
	/**
	 * Read the specified properties file and return it as Properties object
	 * 
	 * @param fileName
	 * @return
	 */
	public Properties readPropertiesFile(String fileName) {
		Properties properties = null;
		InputStream is = null;
		is = getClass().getClassLoader().getResourceAsStream(fileName);
		if (is != null) {
			try {
				properties = new Properties();
				properties.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return properties;
	}

	
	
	/**
	 * Read the modelPathString model file and return it 
	 * as byte array
	 * 
	 * @param modelPathString
	 * @return
	 */
		public byte[] getBytesFromModelFile(String modelPathString) {
			byte[] byteArray = null;
			FileInputStream fis = null;
			File file = new File(modelPathString);
			if (file.exists()) {
				byteArray = new byte[(int) file.length()];
				try {
					fis = new FileInputStream(file);
					fis.read(byteArray);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

				}
			}else{
				throw new RuntimeException("The specified model file "+modelPathString+" does not exist, Please check and try again..!");
			}


			return byteArray;
		}

		
		
	/**
	 * Read the model files from git repository and return map of filename and
	 * model file
	 * 
	 * @param path
	 * @param filenamesExists
	 * @param fileNames
	 * @return
	 */
	public HashMap<String, String> getFilesFromRepository(String path, boolean filenamesExists, String fileNames) {
		HashMap<String, String> modelFiles = new HashMap<String, String>();
		ZipInputStream zipInputStream = null;
		String fileNameArr[] = fileNames.split(",");
		String[] pathParts;
		int pathLen;
		String fName;
		StringWriter stringWriter;
		String fileContent;
		try {
			zipInputStream = new ZipInputStream(new URL(path).openStream());
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {

				if (!zipEntry.isDirectory() && zipEntry.getName().endsWith("xpdl")) {
					pathParts = zipEntry.getName().toString().trim().split("\\/");
					pathLen = pathParts.length;
					fName = pathParts[pathLen - 1].split("\\.")[0];
					stringWriter = new StringWriter();

					IOUtils.copy(zipInputStream, stringWriter);

					fileContent = stringWriter.toString().trim();

					if (filenamesExists) {

						for (String fileName : fileNameArr) {

							if (zipEntry.getName().contains(fileName)) {

								modelFiles.put(fName, fileContent);

							}
						}
					} else {

						modelFiles.put(fName, fileContent);

					}
				}
			}

		} catch (Exception e) {
			LOG.info("getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
					+ e);
			LOG.info("getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
					+ e.getStackTrace());
			LOG.info("getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
					+ e.getCause());
		} finally {
			try {
				zipInputStream.close();
			} catch (IOException e) {
				LOG.info(
						"getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
								+ e);
				LOG.info(
						"getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
								+ e.getStackTrace());
				LOG.info(
						"getFilesFromRepository REST API : Exception in fetching model files for automatic deployment -- "
								+ e.getCause());
			}
		}

		return modelFiles;
	}


	
	
	/**
	 * Read the model files from local File/Folder location and return map of
	 * filename and path to model file
	 * 
	 * @param path
	 * @param dir
	 * @param filenamesExists
	 * @param fileNames
	 * @return
	 */
	public HashMap<String, String> getFilesFromDirectory(String path, File dir, boolean filenamesExists,
			String fileNames) {
		HashMap<String, String> completePaths = new HashMap<String, String>();
		String fileNameArr[] = null;

		if (!filenamesExists) {
			for (File file : dir.listFiles()) {
				if(file.getName().endsWith(".xpdl")){
					fileNames = fileNames + file.getName() + ",";
				}
			}
			fileNames = fileNames.substring(0, fileNames.length() - 1);
		}
		fileNameArr = fileNames.split(",");
		String fName;
		for (String fileName : fileNameArr) {
			fName = fileName.split("\\.")[0];
			completePaths.put(fName, path + fileName.trim());
		}
		return completePaths;
	}
	
	
	/**
	 * Creates UserGroup with the given name, id and description
	 * 
	 * @param name
	 * @param id
	 * @param desc
	 * @return UserGroup
	 */
	public UserGroup createUserGroup(String name, String id, String desc) {
		Date date = new Date();
		UserGroup ug = getUserService().createUserGroup(id, name, desc, date, null);
		return ug;
	}

	/**
	 * Convert the Multi Strip tiff doc content to pdf content and return the
	 * converted byte array
	 * 
	 * @param tiffContent
	 * @return byte[]
	 * @throws IOException
	 * @throws DocumentException
	 */
	public byte[] convertMultiStripTifToPDF(byte[] tiffContent) throws IOException, DocumentException {

		LOG.info("Inside convertMultiStripTifToPDF Method -- conversion Started for a document");

		InputStream inputStream = new ByteArrayInputStream(tiffContent);
		ImageDecoder tiffDecoder = ImageCodec.createImageDecoder("tiff", inputStream, null);
		int numberOfPages = tiffDecoder.getNumPages();

		// creating an output stream to write the converted bytes
		ByteArrayOutputStream outfile = new ByteArrayOutputStream();

		com.itextpdf.text.Document TifftoPDF = new com.itextpdf.text.Document();
		PdfWriter pdfWriter = PdfWriter.getInstance(TifftoPDF, outfile);
		pdfWriter.setStrictImageSequence(true);
		TifftoPDF.open();

		for (int i = 0; i < numberOfPages; i++) {
			RenderedImage rawImageData = tiffDecoder.decodeAsRenderedImage(i);
			RenderedImageAdapter planarImage = new RenderedImageAdapter(rawImageData);
			BufferedImage page = planarImage.getAsBufferedImage();
			Image tempImage = Image.getInstance(page, null);
			// System.out.println(tempImage instanceof ImgRaw);
			tempImage.setAbsolutePosition(0, 0);
			Rectangle pageSize = new Rectangle(tempImage.getWidth(), tempImage.getHeight());
			TifftoPDF.setPageSize(pageSize);
			TifftoPDF.newPage();
			TifftoPDF.add(tempImage);
		}
		TifftoPDF.close();
		outfile.flush();

		LOG.info("Inside convertMultiStripTifToPDF Method -- conversion Ended for a document");

		return outfile.toByteArray();
	}

	/**
	 * Java Method to change the state of an interactive activity of a process
	 * from one state to another
	 * 
	 * @param processOid
	 * @param from
	 * @param to
	 * @return
	 */
	public void changeStatus(long processOid, int from, int to, String activityId) {

		ActivityInstanceQuery query = new ActivityInstanceQuery();
		query = ActivityInstanceQuery.findForProcessInstance(processOid);
		ActivityInstances instances = getQueryService().getAllActivityInstances(query);
		ActivityInstanceState state;
		ActivityInstanceState fromState;
		String currentActivityId ;
		long activityOid ;
		for (ActivityInstance activityInstance : instances) {
			state = activityInstance.getState();
			LOG.info("state: " + state);
			fromState = ActivityInstanceState.getState(from);
			LOG.info("fromState: " + fromState);
			currentActivityId = activityInstance.getActivity().getId();
			if ((state.equals(fromState) || state.equals(ActivityInstanceState.APPLICATION)) && activityInstance.getActivity().isInteractive()
					&& currentActivityId.equals(activityId)) {
				activityOid = activityInstance.getOID();
				LOG.info(activityOid);

				switch (to) {
				case 1:
					getWorkflowService().activate(activityOid);
					break;
				case 5:
					getWorkflowService().suspend(activityOid, null);
					break;
				case 7:
					getWorkflowService().hibernate(activityOid);
					break;
				default:
					getWorkflowService().suspend(activityOid, null);
					break;
				}
			}
		}
	}

	/**
	 * Update the document content from tiff to PDF for all the received
	 * document JCR IDs
	 * 
	 * @param documentIdList
	 * @return
	 */

	public void convertDocTiffToPDFBatch(List<String> documentIdList) {

		LOG.info("Inside convertDocTiffToPDFBatch Method -- Got the List of JCR IDs");

		List<Document> documentList = getDocumentManagementService().getDocuments(documentIdList);

		for (Document doc : documentList) {
			String docName = doc.getName().toLowerCase();
			if(!(docName.endsWith(".tiff") || docName.endsWith(".tif"))) break;

			byte[] docContent = getDocumentManagementService().retrieveDocumentContent(doc.getId());

			byte[] convertedContent = null;
			try {
				convertedContent = convertMultiStripTifToPDF(docContent);
				LOG.info("Inside convertDocTiffToPDFBatch Method -- converted the original byte array of a document: "
						+ doc.getId() + " Name: " + doc.getName());

				if (convertedContent != null) {
					StringBuffer fileName = new StringBuffer(doc.getName());
					doc.setName(fileName.substring(0, fileName.lastIndexOf(".")) + ".pdf");
					LOG.info("Inside convertDocTiffToPDFBatch Method -- Changed the file name of the document: "
							+ doc.getId() + " Name: " + doc.getName());
					getDocumentManagementService().updateDocument(doc, convertedContent, "", true, "", "", false);
				}

			} catch (IOException e) {
				LOG.error(
						"Inside onvertDocTiffToPDFBatch Method -- Got Some errors the conversion is aborted for this document: "
								+ doc.getId() + " Name: " + doc.getName());
				e.printStackTrace();
			} catch (DocumentException e) {
				LOG.error(
						"Inside onvertDocTiffToPDFBatch Method -- Got Some errors the conversion is aborted for this document: "
								+ doc.getId() + " Name: " + doc.getName());
				e.printStackTrace();
			}

		}

	}

	/**
	 * Read the specified External properties file and return it as Properties
	 * object
	 * 
	 * @param fileName
	 * @return Properties
	 */
	public Properties readExternalPropertiesFile(String fileName) {
		Properties properties = null;
		InputStream is = null;
		LOG.info("Reading the Properties file from path = " + fileName);
		try {
			is = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			LOG.info(e1.getMessage());
			LOG.info(e1.getStackTrace());
			LOG.info(e1.getCause());
		}

		if (is != null) {
			try {
				properties = new Properties();
				properties.load(is);
			} catch (IOException e) {
				LOG.info(e.getMessage());
				LOG.info(e.getStackTrace());
				LOG.info(e.getCause());
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					LOG.info(e.getMessage());
					LOG.info(e.getStackTrace());
					LOG.info(e.getCause());
				}
			}
		}
		LOG.info("Returning Properties Object fetched from given fileName");
		return properties;
	}

	/**
	 * Write to the specified External properties file with the properties
	 * object
	 * 
	 * @param fileName
	 * @param properties
	 * @return
	 */
	public void writeExternalPropertiesFile(Properties properties, String fileName) {
		FileOutputStream out = null;
		LOG.info("Writing properties object to the properties file at loaction = " + fileName);
		try {
			out = new FileOutputStream(fileName);
			properties.store(out, null);
		} catch (IOException e) {
			LOG.info(e.getMessage());
			LOG.info(e.getStackTrace());
			LOG.info(e.getCause());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				LOG.info(e.getMessage());
				LOG.info(e.getStackTrace());
				LOG.info(e.getCause());
			}
		}

	}

	/**
	 * Update the document type of a document if its is assigned as null
	 * 
	 * @param documentIdList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void doctypeAssignment(List<String> documentIdList) {
		final String ASSIGN_DOCTYPE = "Correspondence";
		List<Document> documentList = getDocumentManagementService().getDocuments(documentIdList);
		for (Document document : documentList) {
			ArrayList<String> docType = (ArrayList<String>) document
					.getProperty(ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue());
			LOG.info("Document Name = " + document.getName() + " : Document Type = " + docType);
			if (docType == null || docType.isEmpty()) {
				LOG.warn("Assigning the doctype as Correspondence which is null");
				docType = new ArrayList<String>();
				docType.add(ASSIGN_DOCTYPE);
				document.setProperty(ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue(), docType);
				document = getDocumentManagementService().updateDocument(document, true, "updated meta data", "",
						false);
			}
		}

	}

	public String createCSV(String txtCSV) {
		Date currentDate = new Date();
		String fileName = ApplicationConstants.EXCEL_DUMP_NAME.getValue() + currentDate.getTime();
		File file = new File(fileName);
		String result = null;
		try {
			LOG.info(txtCSV);
			PrintWriter pw = new PrintWriter(file);
			StringBuilder sb = new StringBuilder();
			sb.append(txtCSV);
			pw.write(sb.toString());
			pw.close();
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum); // no doubt here is 0
				// Writes len bytes from the specified byte array starting at
				// offset off to this byte array output stream.
				LOG.info("read " + readNum + " bytes,");
			}
			byte[] bytes = bos.toByteArray();
			result = "{\"fileName\":\"" + fileName + "\",\n \"documentContent\":" + Arrays.toString(bytes)
					+ ",\n \"error\":" + null + "}";
		} catch (FileNotFoundException e) {
			LOG.info("Error in Writing to CSV -- " + e.getCause());
			LOG.info("Error in Writing to -- " + e.getMessage());
			LOG.info("Error in Writing to -- " + e.getStackTrace());
			result = "-1";
		} catch (IOException e) {
			LOG.info("Error in Writing to CSV -- " + e.getCause());
			LOG.info("Error in Writing to -- " + e.getMessage());
			LOG.info("Error in Writing to -- " + e.getStackTrace());
			result = "-1";
		}
		return result;
	}

	
	/**
	 * Populate data for split and merge of documents for new Scanning n Indexing
	 * 
	 * @param documentSplitterArray
	 * @return
	 */
	public HashMap<String,HashMap<byte[],int[]>> populateDataForSplitMerge(JsonArray documentSplitterArray){
		HashMap<String,HashMap<byte[],int[]>> formattedSpecificationMap = new HashMap<String,HashMap<byte[],int[]>>();
		HashMap<byte[],HashMap<String,int[]>> specificationMap = new HashMap<byte[],HashMap<String,int[]>>();
		HashMap<String,int[]> innerSpecification;
		JsonArray documentSplitterInnerArray = null;
		JsonArray documentPageNumberArray = null;
		String jcrIdString = "";
		JsonObject splitterOuterObject;
		JsonObject splitterInnerObject;
		String docType ="";
		int[] pgNo ;
		byte[] fileData =null;
		Document currentDocument;
		String documentNameExtn;
		String documentName;
		String extension;
		String[] fileNameArr;
		int nameLength ;
		LOG.info("Populating Data to map for Split and Merge : Started");
		try{
			
		
		for(int i=0;i<documentSplitterArray.size();i++)
		{
			splitterOuterObject = documentSplitterArray.get(i).getAsJsonObject();
			jcrIdString = splitterOuterObject.get("jcrID").getAsString();
			fileData = getDocumentManagementService().retrieveDocumentContent(jcrIdString);
			currentDocument = getDocumentManagementService().getDocument(jcrIdString);
			documentNameExtn = currentDocument.getName();
			fileNameArr = documentNameExtn.split("\\.");
			nameLength = fileNameArr.length;
			extension = fileNameArr[nameLength - 1];
			if (!extension.equalsIgnoreCase("pdf")) {
				if(extension.equalsIgnoreCase("tif") || extension.equalsIgnoreCase("tiff"))
				{
					fileData = convertMultiStripTifToPDF(fileData);
				}
				else if(extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")
						|| extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("gif")){
					fileData = convertImagesToPdf(fileData);
				}else{
					throw new RuntimeException(
							"Invalid document format passed : Splitting and Merging is only supported for PDF, TIFF, JPEG and PNG formats");
				}
			}
			documentSplitterInnerArray = splitterOuterObject.get("docTypes").getAsJsonArray();
			innerSpecification = new LinkedHashMap<String,int[]>();
			for(int j=0;j<documentSplitterInnerArray.size();j++)
			{
				splitterInnerObject = documentSplitterInnerArray.get(j).getAsJsonObject();
				docType = splitterInnerObject.get("docType").getAsString();
				documentPageNumberArray = splitterInnerObject.get("pageNumbers").getAsJsonArray();
				pgNo = new int[documentPageNumberArray.size()];
				for(int k=0;k<documentPageNumberArray.size();k++)
				{
					pgNo[k] = documentPageNumberArray.get(k).getAsInt();
				}
				innerSpecification.put(docType, pgNo);
			}
			specificationMap.put(fileData, innerSpecification);
		}
		
		formattedSpecificationMap = formatSplittingSpecification(specificationMap);
		
		}
		catch(DocumentException e){
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e);
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getCause());
		}
		catch(IOException e){
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e);
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getCause());
		}
		return formattedSpecificationMap;
			
		}
	
	/**
	 * Convert images (jpeg,png) to PDF for new Scanning n Indexing
	 * 
	 * @param originalContent
	 * @return
	 */
	public byte[] convertImagesToPdf(byte[] originalContent){
		LOG.info("Inside convertImgesToPdf Method -- conversion Started for a document");
		byte[] convertedContent = null;
		try {
			com.itextpdf.text.Document document = new com.itextpdf.text.Document();
			PdfWriter writer = null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();			
			Image image = Image.getInstance(originalContent);
			Rectangle A4 = PageSize.A4;
			float scalePortrait = Math.min(A4.getWidth() / image.getWidth(),A4.getHeight() / image.getHeight());
			float scaleLandscape = Math.min(A4.getHeight() / image.getWidth(),A4.getWidth() / image.getHeight());
			boolean isLandscape = scaleLandscape > scalePortrait;
			float w;
		    float h;
		    if (isLandscape) {
		    	A4 = A4.rotate();
		        w = image.getWidth() * scaleLandscape;
		        h = image.getHeight() * scaleLandscape;
		    } else {
		        w = image.getWidth() * scalePortrait;
		        h = image.getHeight() * scalePortrait;
		    }
		    writer = PdfWriter.getInstance(document, bos);
			writer.open();
			document.open();
			image.scaleAbsolute(w, h);
            float posH = (A4.getHeight() - h) / 2;
            float posW = (A4.getWidth() - w) / 2;

            image.setAbsolutePosition(posW, posH);
			image.setBorderWidth(0);
			image.scaleAbsoluteHeight(PageSize.A4.getHeight());
			image.scaleAbsoluteWidth(PageSize.A4.getWidth());
			document.add(image);
			document.close();
			convertedContent = bos.toByteArray();
			writer.close();
			bos.close();
			
		} catch (DocumentException e) {
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e);
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getCause());
		} catch (IOException e) {
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e);
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getCause());
		}
		return convertedContent;
	}

	
	/**
	 * Format the document specification for splitting for new Scanning n Indexing
	 * 
	 * @param originalSpecificationMap
	 * @return
	 */
	public HashMap<String,HashMap<byte[],int[]>> formatSplittingSpecification(HashMap<byte[],HashMap<String,int[]>> originalSpecificationMap){
		HashMap<byte[],int[]> innerMap ;
		HashMap<String,HashMap<byte[],int[]>> formattedSpecificationMap = new HashMap<String,HashMap<byte[],int[]>>();
		byte[] filedata =null;
		String currentDocType= "";
		int[] currentPgNoArr = null;
		
		for (Map.Entry<byte[], HashMap<String,int[]>> originalOuterMap : originalSpecificationMap.entrySet()) 
		{
			filedata = originalOuterMap.getKey();
			for(Map.Entry<String, int[]> originalInnerMap : originalOuterMap.getValue().entrySet())
			{
				currentDocType = originalInnerMap.getKey();
				currentPgNoArr = originalInnerMap.getValue();
				if(formattedSpecificationMap.containsKey(currentDocType))
				{
					innerMap = formattedSpecificationMap.get(currentDocType);
					innerMap.put(filedata, currentPgNoArr);
					formattedSpecificationMap.put(currentDocType, innerMap);
				}
				else
				{
					innerMap = new LinkedHashMap<byte[],int[]>();
					innerMap.put(filedata, currentPgNoArr);
					formattedSpecificationMap.put(currentDocType, innerMap);
				}
			}
			
		}
		return formattedSpecificationMap;
	}
	
	
	/**
	 * Merging multiple PDF's to create a single document for a DocType for new Scanning n Indexing
	 * 
	 * @param formattedSpecificationMap
	 * @param jsonObject
	 * @return
	 */
	public HashMap<String,String> mergePdfDocuments(HashMap<String,HashMap<byte[],int[]>> formattedSpecificationMap, JsonObject jsonObject){
		
		HashMap<String, String> uploadedDocuments = new HashMap<String, String>();
		String docType = "";
		ByteArrayOutputStream outputStream = null;
		PdfWriter pdfWriter = null;
		byte[] content = null;
		int[] pageNumberArray;
		int currentPgNo;
		PdfImportedPage pdfImportedPage;
		PdfContentByte contentByte;
		Rectangle rectangle;
		com.itextpdf.text.Document document ;
		String jcrId ="";
		PdfReader reader;
		LOG.info("Document splitting and merging : Started" + formattedSpecificationMap.toString());
		try {
			for (Map.Entry<String, HashMap<byte[], int[]>> outerEntry : formattedSpecificationMap.entrySet()) {
				docType = outerEntry.getKey().trim();
				document = new com.itextpdf.text.Document();
				outputStream = new ByteArrayOutputStream();
				pdfWriter = PdfWriter.getInstance(document, outputStream);
				document.open();
				contentByte = pdfWriter.getDirectContent();
				for (Map.Entry<byte[], int[]> innerEntry : outerEntry.getValue().entrySet()) {
					content = innerEntry.getKey();
					pageNumberArray = innerEntry.getValue();
					reader = new PdfReader(content);
					for (int number : pageNumberArray) {
						currentPgNo = number;
						rectangle = reader.getPageSize(currentPgNo);
						document.setPageSize(rectangle);
						document.newPage();
						pdfImportedPage = pdfWriter.getImportedPage(reader, currentPgNo);
						contentByte.addTemplate(pdfImportedPage, 0, 0);

					}
					pdfWriter.freeReader(reader);
				}
				document.close();
				
				jcrId = uploadMergedDocument(outputStream.toByteArray(),docType,jsonObject);
				
				uploadedDocuments.put(docType, jcrId);
				
				outputStream.flush();			
				outputStream.close();
				pdfWriter.close();
				LOG.info("Document Type " + docType + " created.");

			}

		} catch (IOException e) {
			LOG.info("Merging Docuemnts  : Exception MergeDocuments -- " + e);
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getStackTrace());
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getCause());

		} catch (DocumentException e) {
			LOG.info("Merging Docuemnts  : Exception MergeDocuments -- " + e);
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getStackTrace());
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getCause());

		}
		return uploadedDocuments;

	}
	
	/**
	 * Uploading the merged PDF documents for new Scanning n Indexing
	 * 
	 * @param documentContent
	 * @param docType
	 * @param jsonObject
	 * @return
	 */
	public String uploadMergedDocument(byte[] documentContent,String docType, JsonObject jsonObject){
		LOG.info("Uploading the merged documents : Started");
		String jcrId = "";
		String schemeNo = "schemeNo";
		String memberNo ="memberNo";
		String workType = "";
		Long processOid =0L;
		String fileName = docType;
		
		if (jsonObject.get("schemeNo") != null) {
			schemeNo = jsonObject.get("schemeNo").getAsString();
			fileName = fileName +"-"+schemeNo;
		}
		if (jsonObject.get("memberNo") != null) {
			memberNo = jsonObject.get("memberNo").getAsString();	
			fileName = fileName +"-"+memberNo;
		}
		if (jsonObject.get("workType") != null) {
			workType = jsonObject.get("workType").getAsString();			
		}
		if (jsonObject.get("processOID") != null) {
			processOid = jsonObject.get("processOID").getAsLong();			
		}
		
		Map<String,Object> metaDataMap = this.populateDocumentProperties(schemeNo,memberNo,workType,docType);
		fileName = fileName + ".pdf";
		fileName=fileName.replaceAll("[\\\\|?:\"<>\\/*]+","_");
		String contentType = URLConnection.guessContentTypeFromName(fileName);
		
		
		Document document = this.saveDocumentinIpp(documentContent, fileName, contentType,
				processOid, metaDataMap);
		jcrId = document.getId();
		
		LOG.info("After uploading the document : "+jcrId + "for DocType : " +docType);
		return jcrId;
	}

}

