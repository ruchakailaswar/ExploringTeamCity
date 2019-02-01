package com.psl.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.stardust.common.error.AccessForbiddenException;
import org.eclipse.stardust.common.error.ObjectNotFoundException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.ProcessInstance;
import org.eclipse.stardust.engine.api.runtime.ProcessInstanceState;
import org.eclipse.stardust.engine.api.runtime.ServiceFactory;
import org.eclipse.stardust.engine.api.runtime.ServiceFactoryLocator;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;
import org.eclipse.stardust.engine.core.runtime.beans.AbortScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.psl.beans.ApplicationConstants;

@Service
public class ProcessMigration {

	@Autowired
	IppService ippService;

	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	static final Logger LOG = LogManager.getLogger(ProcessMigration.class);

	/**
	 * Method used in Process Migration Process to parse the CSV file which has
	 * the details of the process to be migrated
	 * 
	 * @param path
	 *            Path of the file where it is stored
	 * @return
	 */
	public Map parseCSV(String path) {

		LOG.info(path);
		Map processDetails = new HashMap();
		Map csvData = null;
		List<Map> processList = new ArrayList();
		int i = 0;
		int j = 0;
		String[] keyArr = ApplicationConstants.EXCEL_DUMP_KEYS.getValue().split(",");
		try {
			File file = new File(path);
			LOG.info("Inside parseCSV : " + file);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			String[] tempArr;
			while ((line = br.readLine()) != null) {
				LOG.info("Inside parseCSV : " + line);
				if (i == 0) {
					i++;
					continue;
				}
				csvData = new HashMap();
				tempArr = line.split(",");
				for (j = 0; j < keyArr.length; j++) {
					csvData.put(keyArr[j], tempArr[j]);
				}
				processList.add(csvData);

			}
			br.close();
			processDetails.put("processInstance", processList);
		} catch (Exception e) {
			LOG.info("Error in Parsing CSV -- " + e.getCause());
			LOG.info("Error in Parsing CSV -- " + e.getMessage());
			LOG.info("Error in Parsing CSV -- " + e.getStackTrace());

		}

		return processDetails;

	}

	/**
	 * Method called in Process Migration Process to abort the existing instance
	 * and return the document details of the documents that were attached to
	 * the case
	 * 
	 * @param processDetail
	 *            Details of the process to be aborted
	 * @param hierarchy
	 *            hierarchy for aborting the process
	 * @return
	 */
	public Map abortActiveProcess(Map<String, List> processDetail, String hierarchy, String username, String password,
			long currentProcessOID) {

		LOG.info("Inside abortActiveProcess, starting user : " + username);
		LOG.info("Inside abortActiveProcess : " + processDetail);

		ServiceFactory sf = ServiceFactoryLocator.get(username, password);
		WorkflowService wf = sf.getWorkflowService();
		String txtOutput = null;
		long processOID = 0;
		boolean result = false;
		String processState = null;
		ProcessInstance pi = null;
		List<Map> processList = null;
		JsonObject docDetails;
		JsonArray responseArray;
		processList = processDetail.get("processInstance");
		for (Map csvData : processList) {
			try {

				if (csvData.get("processInstanceOID") != null) {
					processOID = Long.parseLong(csvData.get("processInstanceOID").toString());
					if (processOID < currentProcessOID) {
						pi = wf.getProcessInstance(processOID);
						processState = pi.getState().getName();
						result = abortProcess(hierarchy, processOID, processState, wf);
						if (result) {
							if (csvData.get("action").toString().equalsIgnoreCase("re-trigger")) {
								responseArray = new JsonArray();
								List<Document> docList = ippService.getAttachedDocuments(processOID);
								responseArray = getDocumentDetailsObject(docList);
								if (responseArray != null) {
									docDetails = new JsonObject();
									docDetails.add("documents", responseArray);
									csvData.put("indexingRequestDetails", docDetails.toString());
								}

							}
						}
					}
				}
			} catch (ObjectNotFoundException e) {
				LOG.info("Inside abortActiveProcess - Cannot abort process for process OID : " + processOID + "\n"
						+ e.getCause());
				LOG.info("Inside abortActiveProcess - Cannot abort process for process OID : " + processOID + "\n"
						+ e.getMessage());
				LOG.info("Inside abortActiveProcess - Cannot abort process for process OID : " + processOID + "\n"
						+ e.getStackTrace());
			} finally {
				csvData.put("isAborted", result);
			}
		}
		LOG.info("Completed abortActiveProcess Method " + processDetail);
		return processDetail;
	}

	/**
	 * 
	 * @param docList
	 *            Returns the JSON Object of the documents attached in the
	 *            existing case
	 * @return
	 */
	public JsonArray getDocumentDetailsObject(List<Document> docList) {
		JsonArray responseArray = new JsonArray();
		JsonObject responseObject;
		String txtDocType = "Other";
		for (Document document : docList) {

			responseObject = new JsonObject();
			responseObject.addProperty("name", document.getName());
			responseObject.addProperty("creationTimestamp", document.getDateCreated().getTime());
			responseObject.addProperty("contentType", document.getContentType());
			responseObject.addProperty("path", document.getPath());
			responseObject.addProperty("uuid", document.getId());
			responseObject.addProperty("numPages", 0);

			// Parse document to String
			txtDocType = ippService.getDocumentTypeString(document);
			JsonObject docType = new JsonObject();
			docType.addProperty("name", txtDocType);
			responseObject.add("documentType", docType);

			responseObject.addProperty("pageCount", 0);
			// Commented as not a mandatory parameter
			// responseObject.addProperty("url",
			// ApplicationConstants.URL_STRING.getValue() + document.getId());
			responseObject.addProperty("pages", "[]");

			responseArray.add(responseObject);
		}
		return responseArray;
	}

	/**
	 * 
	 * @param heirarchy
	 *            process hierarchy of the process to be aborted
	 * @param processInstanceOID
	 *            Process OID of the process
	 * @param processState
	 *            Current State of the process instance
	 * @param sf
	 * @return
	 * @throws ObjectNotFoundException
	 */
	public boolean abortProcess(String heirarchy, long processInstanceOID, String processState, WorkflowService wf) {
		LOG.info("Inside abortProcessInstances - " + processInstanceOID + heirarchy);
		ProcessInstance abortedProcess = null;
		boolean isAborted = true;
		try {
			if (!(processState.equals(ProcessInstanceState.Aborted.toString()))
					&& !(processState.equals(ProcessInstanceState.Completed.toString()))
					&& !(processState.equals(ProcessInstanceState.Aborting.toString()))) {
				if (heirarchy.equals("subProcess")) {
					abortedProcess = wf.abortProcessInstance(processInstanceOID, AbortScope.SubHierarchy);
				} else {
					abortedProcess = wf.abortProcessInstance(processInstanceOID, AbortScope.RootHierarchy);
				}

			}
		} catch (ObjectNotFoundException e) {
			isAborted = false;
			LOG.info("Cannot Abort Process for Process OID : " + processInstanceOID);
			LOG.info("Cannot Abort Process for Process OID : " + e.getCause());
			LOG.info("Cannot Abort Process for Process OID : " + e.getMessage());
			LOG.info("Cannot Abort Process for Process OID : " + e.getStackTrace());
		} catch (AccessForbiddenException e) {
			isAborted = false;
			LOG.info("Cannot Abort Process for Process OID : " + processInstanceOID);
			LOG.info("Cannot Abort Process for Process OID : " + e.getCause());
			LOG.info("Cannot Abort Process for Process OID : " + e.getMessage());
			LOG.info("Cannot Abort Process for Process OID : " + e.getStackTrace());
		} 
			return isAborted;
	}

}
