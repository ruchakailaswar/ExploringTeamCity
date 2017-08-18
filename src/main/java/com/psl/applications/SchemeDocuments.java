package com.psl.applications;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.BadRequestException;

import org.eclipse.stardust.common.annotations.ParameterName;
import org.eclipse.stardust.common.error.InvalidValueException;
import org.eclipse.stardust.common.error.ObjectNotFoundException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.query.DataFilter;
import org.eclipse.stardust.engine.api.query.DescriptorPolicy;
import org.eclipse.stardust.engine.api.query.ProcessInstanceQuery;
import org.eclipse.stardust.engine.api.query.ProcessInstances;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.DocumentManagementServiceException;
import org.eclipse.stardust.engine.api.runtime.ProcessInstance;
import org.eclipse.stardust.engine.api.runtime.ServiceFactory;
import org.eclipse.stardust.engine.api.runtime.ServiceFactoryLocator;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;

import com.psl.beans.ApplicationConstants;

public class SchemeDocuments {

	private String ippPassword = "motu";
	private String ippUser = "motu";

	static final Logger LOG = LogManager.getLogger(SchemeDocuments.class);

	public String AttachSchemeDocumentToCurrentProcess(@ParameterName("productType") String productType,
			@ParameterName("schemeNo") String schemeNo, @ParameterName("workType") String workType,
			@ParameterName("targetProcessOID") Long targetProcessOID) {
		String responseStatus = "\"Failure\"", response = null, processId = null;
		ProcessInstances pis = null;
		List<Document> docsList = null;
		Properties prop = null;
		InputStream is = null;
		String[] worktypesArr;
		ServiceFactory sf = ServiceFactoryLocator.get(ippUser, ippPassword);
		WorkflowService ws = sf.getWorkflowService();
		try {
			is = getClass().getClassLoader().getResourceAsStream("producttype-processid-map.properties");
			if (is != null) {
				prop = new Properties();
				prop.load(is);
				worktypesArr = prop.getProperty("workTypes").split(",");

				List<String> worktypesList = Arrays.asList(worktypesArr);

				if (worktypesList.contains(workType)) {

					processId = prop.getProperty(productType);
					if (processId != null && schemeNo != null && targetProcessOID != 0L) {
						ProcessInstanceQuery piQuery = ProcessInstanceQuery.findForProcess(processId, false);
						piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);

						String[] dataElement = prop.getProperty(productType + "_PROCESS_DATAPATH").split("\\.");
						if (dataElement.length > 1) {
							piQuery.where(DataFilter.isEqual(dataElement[0], dataElement[1], schemeNo));
						} else {
							piQuery.where(DataFilter.isEqual(dataElement[0], schemeNo));
						}
						pis = sf.getQueryService().getAllProcessInstances(piQuery);
						if (pis != null && pis.size() > 0) {
							for (ProcessInstance pi : pis) {
								LOG.warn("Selected OID :: " + pi.getOID());
								docsList = new ArrayList<Document>();
								List<Document> processAttachments = null;
								try {
									Object processData = ws.getInDataPath(pi.getOID(),
											ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
									processAttachments = (List<Document>) processData;
									if (processAttachments != null && !processAttachments.isEmpty()) {
										LOG.warn(
												"GetDocumentListService NEW REST API : processAttachments for processOID "
														+ pi.getOID() + " are " + processAttachments.size());
										for (Document processAttachment : processAttachments) {
											LOG.warn("GetDocumentListService NEW REST API : Document Detail -- "
													+ processAttachment.getId() + " " + processAttachment.getName()
													+ " " + processAttachment.getDateCreated());
											LOG.warn(
													"Fetching a processAttachments for processOID " + pi.getOID());
											docsList.add(processAttachment);
										}
									} else {
										LOG.warn(
												"GetDocumentListService NEW REST API : processAttachments for processOID "
														+ pi.getOID() + " are empty!");
									}
								} catch (ObjectNotFoundException e) {
									LOG.warn("Could not find Data Path "
											+ ApplicationConstants.PROCESS_ATTACHMENTS.getValue()
											+ " on scope process Instance " + pi.getOID());
								} catch (InvalidValueException e) {
									LOG.warn("Could not get data for Data Path "
											+ ApplicationConstants.PROCESS_ATTACHMENTS.getValue()
											+ " on scope process Instance " + pi.getOID());
								}
								if (docsList != null && docsList.size() > 0) {
									break;
								}
								LOG.warn("And the document attached to process are :: " + docsList);
							}
							if (docsList != null && docsList.size() > 0) {
								Object processData = ws.getInDataPath(targetProcessOID,
										ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
								if (processData != null) {
									LOG.warn("Previuos Attachments found in process and need to consider them as well: "
											+ processData.getClass());
									docsList.addAll((List) processData);
								}
								ws.setOutDataPath(targetProcessOID, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(),
										docsList);
								responseStatus = "\"Success\"";
								response = "{\"response\":" + responseStatus + "}";
								return response;
							} else {
								LOG.warn("No instance with attached document found for provided input");
								response = "{\"response\":" + responseStatus + "}";
								return response;
							}
						} else {
							LOG.warn("No instance found for provided input");
							response = "{\"response\":" + responseStatus + "}";
							return response;
						}
					} else {
						LOG.warn(processId == null ? "processId"
								: schemeNo == null ? "schemeNo" : "targetProcessOID" + " Not Found");
						response = "{\"response\":" + responseStatus + "}";
						return response;

					}
				} else {
					LOG.warn("Not supported for current work type");
					responseStatus = "\"Success\"";
					response = "{\"response\":" + responseStatus + "}";
					return response;
				}
			} else {
				LOG.warn("Inputstream Object for reading properties file is null");
				response = "{\"response\":" + responseStatus + "}";
				return response;
			}
		} catch (ObjectNotFoundException e) {
			LOG.warn("Could not find Data Path PROCESS_ATTACHMENTS on scope process Instance " + targetProcessOID);
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		} catch (InvalidValueException e) {
			LOG.warn("Could not set data for Data Path PROCESS_ATTACHMENTS on scope process Instance "
					+ targetProcessOID);
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		} catch (DocumentManagementServiceException e) {
			LOG.warn("Could not set data for Data Path PROCESS_ATTACHMENTS on scope process Instance "
					+ targetProcessOID);
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		} catch (BadRequestException e) {
			LOG.warn("Please check input parameters processId : " + processId + ", schemeNo : " + schemeNo
					+ ", targetProcessOID : " + targetProcessOID + e.getStackTrace());
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		} catch (FileNotFoundException e) {
			LOG.warn("Unable to locate property file 'Producttype-processid-map.properties' in classpath"
					+ e.getStackTrace());
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		} catch (Exception e) {
			LOG.warn("Exception inside getProcessInstanceByProcessIdAndSchemeNo  REST API : Getting Process instance !"
					+ e.getStackTrace());
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;
		}
	}

}
