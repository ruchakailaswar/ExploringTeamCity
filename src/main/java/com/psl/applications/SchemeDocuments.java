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
		ServiceFactory sf = ServiceFactoryLocator.get(ippUser, ippPassword);
		WorkflowService ws = sf.getWorkflowService();
		try {
			LOG.warn("AttachSchemeDocumentToCurrentProcess\n Product Type : " + productType + ", Scheme No : "
					+ schemeNo + "Work Type : " + workType + "Target Process OID : " + targetProcessOID);

			is = getClass().getClassLoader().getResourceAsStream("producttype-processid-map.properties");
			if (is != null) {
				prop = new Properties();
				prop.load(is);
				String[] worktypesArr = prop.getProperty("workTypes").split(",");
				worktypesArr = prop.getProperty("workTypes").split(",");

				List<String> worktypesList = Arrays.asList(worktypesArr);

				if (worktypesList.contains(workType)) {

					processId = prop.getProperty(productType);
					if (processId != null && schemeNo != null && targetProcessOID != 0L) {

						LOG.warn("AttachSchemeDocumentToCurrentProcess\n Fetching Process Instance");
						ProcessInstanceQuery piQuery = ProcessInstanceQuery.findForProcess(processId, false);
						piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);

						String[] dataElement = prop.getProperty(productType + "_PROCESS_DATAPATH").split("\\.");
						piQuery.where(DataFilter.isEqual(dataElement[0], dataElement[1], schemeNo));
						pis = sf.getQueryService().getAllProcessInstances(piQuery);
						LOG.warn("AttachSchemeDocumentToCurrentProcess\n No Of Instances found : " + pis.getSize());

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
												"AttachSchemeDocumentToCurrentProcess : Number of attachments for processOID "
														+ pi.getOID() + " are " + processAttachments.size());
										for (Document processAttachment : processAttachments) {
											LOG.warn(
													"AttachSchemeDocumentToCurrentProcess : Document Detail -- , Doc Id : "
															+ processAttachment.getId() + ", Name "
															+ processAttachment.getName() + ", Date Created "
															+ processAttachment.getDateCreated() + ", Type : "
															+ processAttachment.getDocumentType());
											docsList.add(processAttachment);
										}
									} else {
										LOG.warn(
												"AttachSchemeDocumentToCurrentProcess : No attachments available for processOID "
														+ pi.getOID() + "!!!!");
									}
								} catch (ObjectNotFoundException e) {
									LOG.warn("AttachSchemeDocumentToCurrentProcess : Could not find Data Path "
											+ ApplicationConstants.PROCESS_ATTACHMENTS.getValue()
											+ " on scope process Instance " + pi.getOID());
								} catch (InvalidValueException e) {
									LOG.warn("AttachSchemeDocumentToCurrentProcess : Could not get data for Data Path "
											+ ApplicationConstants.PROCESS_ATTACHMENTS.getValue()
											+ " on scope process Instance " + pi.getOID());
								}
								if (docsList != null && docsList.size() > 0) {
									break;
								}
								LOG.warn(
										"AttachSchemeDocumentToCurrentProcess : the document/s attached to the process are :: "
												+ docsList);
							}
							if (docsList != null && docsList.size() > 0) {
								Object processData = ws.getInDataPath(targetProcessOID,
										ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
								if (processData != null) {
									LOG.warn(
											"AttachSchemeDocumentToCurrentProcess : Adding the existing attachments to the temp doc list "
													+ processData.getClass());
									docsList.addAll((List) processData);
								}
								ws.setOutDataPath(targetProcessOID, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(),
										docsList);
								responseStatus = "\"Success\"";
								return "{\"response\":" + responseStatus + "}";
							} else {
								LOG.warn(
										"AttachSchemeDocumentToCurrentProcess : No instance with attached document found for provided input");
								return "{\"response\":" + responseStatus + "}";
							}
						} else {
							LOG.warn("AttachSchemeDocumentToCurrentProcess : No instance found for provided input");
							return "{\"response\":" + responseStatus + "}";
						}
					} else {
						LOG.warn(schemeNo == null ? "schemeNo"
								: "AttachSchemeDocumentToCurrentProcess : " + processId == null ? "processId"
										: "targetProcessOID Not Found");
						return "{\"response\":" + responseStatus + "}";
					}
				} else {
					LOG.warn("AttachSchemeDocumentToCurrentProcess : Not supported for current work type");
					responseStatus = "\"Success\"";
					return "{\"response\":" + responseStatus + "}";
				}
			} else {
				LOG.warn(
						"AttachSchemeDocumentToCurrentProcess : Inputstream Object for reading properties file is null");
				return "{\"response\":" + responseStatus + "}";
			}
		} catch (ObjectNotFoundException e) {
			LOG.warn(
					"AttachSchemeDocumentToCurrentProcess : Could not find Data Path PROCESS_ATTACHMENTS on scope process Instance "
							+ targetProcessOID);
			e.printStackTrace();
			return "{\"response\":" + responseStatus + "}";
		} catch (InvalidValueException e) {
			LOG.warn(
					"AttachSchemeDocumentToCurrentProcess : Could not set data for Data Path PROCESS_ATTACHMENTS on scope process Instance "
							+ targetProcessOID);
			e.printStackTrace();
			return "{\"response\":" + responseStatus + "}";
		} catch (DocumentManagementServiceException e) {
			LOG.warn(
					"AttachSchemeDocumentToCurrentProcess : Could not set data for Data Path PROCESS_ATTACHMENTS on scope process Instance "
							+ targetProcessOID);
			e.printStackTrace();
			return "{\"response\":" + responseStatus + "}";
		} catch (BadRequestException e) {
			LOG.warn("AttachSchemeDocumentToCurrentProcess : Please check input parameters processId : " + processId
					+ ", schemeNo : " + schemeNo + ", targetProcessOID : " + targetProcessOID + e.getStackTrace());
			e.printStackTrace();
			return "{\"response\":" + responseStatus + "}";
		} catch (FileNotFoundException e) {
			LOG.warn(
					"AttachSchemeDocumentToCurrentProcess : Unable to locate property file 'Producttype-processid-map.properties' in classpath"
							+ e.getStackTrace());
			e.printStackTrace();
			return "{\"response\":" + responseStatus + "}";
		} catch (Exception e) {
			LOG.warn(
					"AttachSchemeDocumentToCurrentProcess : Exception inside getProcessInstanceByProcessIdAndSchemeNo  REST API : Getting Process instance !"
							+ e.getStackTrace());
			e.printStackTrace();
			response = "{\"response\":" + responseStatus + "}";
			return response;

		}

	}

}
