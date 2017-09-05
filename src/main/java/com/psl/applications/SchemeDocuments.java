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
import org.springframework.beans.factory.annotation.Autowired;

import com.psl.beans.ApplicationConstants;

public class SchemeDocuments {

	@Autowired
	IppService ippService;

	public IppService getIppService() {
		return this.ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	private String ippPassword = "motu";
	private String ippUser = "motu";

	static final Logger LOG = LogManager.getLogger(SchemeDocuments.class);

	public String AttachSchemeDocumentToCurrentProcess(@ParameterName("productType") String productType,
			@ParameterName("schemeNo") String schemeNo, @ParameterName("workType") String workType,
			@ParameterName("targetProcessOID") Long targetProcessOID) {
		String responseStatus = "\"Failure\"";
		String response = null;
		String processId = null;
		ProcessInstances pis = null;
		List<Document> docsList = null;
		Properties prop = null;
		InputStream is = null;
		ServiceFactory sf = ServiceFactoryLocator.get(this.ippUser, this.ippPassword);
		WorkflowService ws = sf.getWorkflowService();
		try {
			LOG.warn("AttachSchemeDocumentToCurrentProcess\n Product Type : " + productType + ", Scheme No : "
					+ schemeNo + "Work Type : " + workType + "Target Process OID : " + targetProcessOID);

			is = getClass().getClassLoader().getResourceAsStream("producttype-processid-map.properties");
			if (is != null) {
				prop = new Properties();
				prop.load(is);
				String[] worktypesArr = prop.getProperty("workTypes").split(",");

				List<String> worktypesList = Arrays.asList(worktypesArr);

				if (worktypesList.contains(workType)) {
					String docTypes = prop.getProperty("docType");
					processId = prop.getProperty(productType);
					String[] dataElement = prop.getProperty("PROCESS_DATAPATH").split("\\.");
					if (processId != null && schemeNo != null && targetProcessOID.longValue() != 0L) {

						LOG.warn("AttachSchemeDocumentToCurrentProcess\n Fetching Process Instance");

						if (productType.equalsIgnoreCase("Liber8")) {
							String[] processIDS = processId.split(",");
							pis = fetchProcessInstances(productType, schemeNo, processIDS[0], dataElement, sf);
							if ((pis == null) || (pis.size() == 0)) {
								pis = fetchProcessInstances(productType, schemeNo, processIDS[1], dataElement, sf);
								if ((pis == null) || (pis.size() == 0)) {
									processId = prop.getProperty("Tier1");
									pis = fetchProcessInstances("Tier1", schemeNo, processId, dataElement, sf);
								}
							}
						} else {
							pis = fetchProcessInstances(productType, schemeNo, processId, dataElement, sf);
						}

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

											if (processAttachment.getProperty(
													ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue()) == null) {
												LOG.warn(processAttachment.getProperties() + "-----------"
														+ processAttachment.getProperty(
																ApplicationConstants.META_DATA_DOCUMENT_TYPES
																		.getValue()));

												processAttachment.setProperty(
														ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue(),
														"New Business");

												LOG.warn(processAttachment.getProperties() + "-----------"
														+ processAttachment.getProperty(
																ApplicationConstants.META_DATA_DOCUMENT_TYPES
																		.getValue()));
											}

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
								Object processData = ws.getInDataPath(targetProcessOID.longValue(),
										ApplicationConstants.PROCESS_ATTACHMENTS.getValue());
								if (processData != null) {
									LOG.warn(
											"AttachSchemeDocumentToCurrentProcess : Adding the existing attachments to the temp doc list "
													+ processData.getClass());
									docsList.addAll((List) processData);
								}
								ws.setOutDataPath(targetProcessOID.longValue(), ApplicationConstants.PROCESS_ATTACHMENTS.getValue(),
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

	private ProcessInstances fetchProcessInstances(String productType, String schemeNo, String processId,
			String[] dataElement, ServiceFactory sf) {
		LOG.warn("AttachSchemeDocumentToCurrentProcess" + processId);

		ProcessInstanceQuery piQuery = ProcessInstanceQuery.findForProcess(processId, false);
		piQuery.setPolicy(DescriptorPolicy.WITH_DESCRIPTORS);

		piQuery.where(DataFilter.isEqual(dataElement[0], dataElement[1], schemeNo));

		LOG.warn("AttachSchemeDocumentToCurrentProcess" + piQuery);

		ProcessInstances pis = sf.getQueryService().getAllProcessInstances(piQuery);
		return pis;
	}

}
