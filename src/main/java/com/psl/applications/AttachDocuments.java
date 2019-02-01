package com.psl.applications;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.stardust.common.annotations.ParameterName;
import org.eclipse.stardust.common.error.ObjectNotFoundException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.ServiceFactory;
import org.eclipse.stardust.engine.api.runtime.ServiceFactoryLocator;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

import com.psl.beans.ApplicationConstants;

public class AttachDocuments {

	
	static final Logger LOG = LogManager.getLogger(AttachDocuments.class);
	
	@Autowired
	IppService ippService;
	
	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}
	

/**
 * Attaches all the claims documents to IFP - New Business Process and Switch Form to the Switches & Election Process
 * @param sourceProcessOID process OID of the (source)Claims or IFP - NB Process
 * @param targetProcessOID process OID of the (target)IFP - NB or Switch Process
 * @param workType Switch or Claims
 */

	@SuppressWarnings("unchecked")
	public void attachDocuments(@ParameterName("sourceProcessOID") long sourceProcessOID,
			@ParameterName("targetProcessOID") long targetProcessOID, @ParameterName("workType") String workType) {
		LOG.info("Inside method attachDocuments - Source Process OID : " + sourceProcessOID + "Target Process OID : "
				+ targetProcessOID);
		WorkflowService ws = null;
		if (sourceProcessOID != targetProcessOID) {
			if( ippService == null){
				ServiceFactory sf = ServiceFactoryLocator.get("motu", "motu");
				ws = sf.getWorkflowService();
			}
			else {
				 ws = ippService.getWorkflowService();
			}
		
			
			try {
				String documentType = null;
				List<Document> docsList = null;
				List<Document> processAttachments = null;
				Object attachmentsInData = ws.getInDataPath(sourceProcessOID,
						ApplicationConstants.PROCESS_ATTACHMENTS.getValue());

				processAttachments = (List<Document>) attachmentsInData;
				docsList = new ArrayList<Document>();
				for (Document processAttachment : processAttachments) {
					LOG.warn("AttachSchemeDocumentToCurrentProcess : Document Detail -- , Doc Id : "
							+ processAttachment.getId() + ", Name " + processAttachment.getName() + ", Date Created "
							+ processAttachment.getDateCreated() + ", Type : " + processAttachment.getDocumentType());

					documentType = processAttachment
							.getProperty(ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue()).toString();

					if (workType != null && workType.equalsIgnoreCase(ApplicationConstants.SWITCHES_AND_ELECTIONS.getValue())
							&& documentType.indexOf(ApplicationConstants.SWITCH_FORM_DOC_TYPE.getValue()) > -1) {
						docsList.clear();
						docsList.add(processAttachment);
						break;

					}

					LOG.warn(processAttachment.getProperties() + "-----------"
							+ processAttachment.getProperty(ApplicationConstants.META_DATA_DOCUMENT_TYPES.getValue()));

					docsList.add(processAttachment);
				}

				LOG.info("Inside method attachDocuments - Attached Documents : Count : " + docsList.size()
						+ "Documents : " + docsList);
				if (docsList != null && docsList.size() > 0) {
					ws.setOutDataPath(targetProcessOID, ApplicationConstants.PROCESS_ATTACHMENTS.getValue(), docsList);
				}
			} catch (ObjectNotFoundException e) {
				LOG.info("Could not find data path for process OID : " + sourceProcessOID);
			}
		}

	}

}
