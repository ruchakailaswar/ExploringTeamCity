package com.psl.services;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;

import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.psl.applications.IppService;

@Component
@Path("/getdocuments")
public class GetDocumentListService {
	
    public static final String META_DATA_SCHEME_NO = "SchemeNo";
    public static final String META_DATA_MEMBER_NO = "MemberNo";
    public static final String META_DATA_REQUEST_TYPE = "RequestType";
    public static final String META_DATA_DOCUMENT_TYPES = "DocumentType";
	@Autowired
	IppService ippService;

	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	private static final Logger LOG = LogManager.getLogger(GetDocumentListService.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("process-instances/{oid: \\d+}/GetdocumentsForAProcess")
	public Response getProcessDocumentsIPPForAProcess(@PathParam("oid") String processOid) {
		try {

			LOG.info("GetDocumentListService  REST API : Get documents For A Process !");
			List<Document> docsList = new ArrayList<Document>();

			Long processOID = 0L;

			try {
				processOID = Long.parseLong(processOid);

				docsList = ippService.getAttachedDocuments(processOID);

			} catch (Exception e) {
				LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 1 -- " + e);
				LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 1-- "
						+ e.getStackTrace());
				LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 1-- "
						+ e.getCause());
				e.printStackTrace();
				return Response.ok("There are No Documents Found!").build();
			}

			return ippService.prepareJSONData(docsList);

		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 2 -- " + e);
			LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 2-- "
					+ e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception getProcessDocumentsIPPForAProcess 2-- "
					+ e.getCause());
			e.printStackTrace();
			return Response.ok("No Documents Found. Some Exceptions!").build();
		}
	}

	@POST
	@Path("process-instances/{oid: \\d+}/{docType}/adddocuments")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addFiles(@PathParam("oid") String processOid, @PathParam("docType") String docType,
			Attachment attachment, @Context HttpServletRequest request) {

		LOG.info("GetDocumentListService REST API : Started Upload Files using CXF !");

		JsonObject response = null;
		StringTokenizer st = new StringTokenizer(docType, "|");
		String[] dt = new String[st.countTokens()];
		int index = 0;
		while (st.hasMoreTokens()) {
			dt[index] = st.nextToken();
			index++;
		}

		DataHandler dataHandler = attachment.getDataHandler();
		try {
			InputStream inputStream = dataHandler.getInputStream();
			MultivaluedMap<String, String> headers = attachment.getHeaders();
			FileInfo fileInfo = getFileName(headers);

			byte[] documentContent = IOUtils.toByteArray(inputStream);
			LOG.info(Arrays.toString(documentContent));
			String contentType = URLConnection.guessContentTypeFromName(fileInfo.name);
			Map<String, Object> properties = new HashMap<String, Object>();
			List<String> docTypes = new ArrayList<String>();

			for (String type : dt)
				docTypes.add(type);

			properties.put("DocumentType", docTypes);

			Document document = ippService.saveDocumentinIpp(documentContent, fileInfo.name, contentType,
					Long.parseLong(processOid), properties);

			inputStream.close();

			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", document.getId());
			response.addProperty("DocumentTypes", ippService.getDocumentTypeString(document));

		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception addFiles -- " + e.getMessage());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).build();
		} else {
			return Response.serverError().build();
		}
	}

	private class FileInfo {
		String name;
		String contentType;

		public FileInfo() {
			name = "unknown";
			contentType = "unknown";
		}
	}

	private FileInfo getFileName(MultivaluedMap<String, String> header) {
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

		FileInfo fileInfo = new FileInfo();
		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {
				String[] name = filename.split("=");
				fileInfo.name = name[1].trim().replaceAll("\"", "");
			}
		}

		fileInfo.contentType = header.getFirst("Content-Type");

		return fileInfo;
	}

	@POST
	@Path("process-instances/{oid: \\d+}/{docType}/{docId}/{memberNo}/{schemeNo}/{workType}/editdocuments")
	@Produces(MediaType.APPLICATION_JSON)
	public Response editDocumentTypes(@PathParam("oid") String processOid, @PathParam("docType") String docType,
			@PathParam("docId") String docId, @PathParam("memberNo") String memberNo,
			@PathParam("schemeNo") String schemeNo, @PathParam("workType") String workType) {

		LOG.info("GetDocumentListService REST API : Started Edit Document Types !");
		LOG.info("GetDocumentListService REST API - docTypes: " + docType);
		LOG.info("GetDocumentListService REST API - docId: " + docId);

		JsonObject response = null;
		StringTokenizer st = new StringTokenizer(docType, "|");
		String[] dt = new String[st.countTokens()];
		int index = 0;
		while (st.hasMoreTokens()) {
			dt[index] = st.nextToken();
			index++;
		}

		try {
			List<String> docTypes = new ArrayList<String>();
			for (String type : dt)
				docTypes.add(type);

			Map<String, Object> metaDataMap = new HashMap<String, Object>();
			metaDataMap.put(META_DATA_SCHEME_NO, schemeNo);
			metaDataMap.put(META_DATA_MEMBER_NO, memberNo);
			metaDataMap.put(META_DATA_REQUEST_TYPE, workType);
			metaDataMap.put(META_DATA_DOCUMENT_TYPES, docTypes);

			//metaDataMap.put("DocumentType", docTypes);
			
			Document document = ippService.getDocumentManagementService().getDocument(docId);

			LOG.info("GetDocumentListService Edit REST API - docName: " + document.getName());
			LOG.info("GetDocumentListService Edit REST API - docId: " + document.getId());
			LOG.info("GetDocumentListService Edit REST API - Properties: " + document.getProperties());
			
			
			Document returnDoc = ippService.updateNonSecureDocument(docId,Long.parseLong(processOid), metaDataMap);
			LOG.info("GetDocumentListService Edit REST API Returned Doc- Doc name : " + returnDoc.getName());
			LOG.info("GetDocumentListService Edit REST API Returned Doc - docId: " + returnDoc.getId());
			LOG.info("GetDocumentListService Edit REST API Returned Doc - properties : " + returnDoc.getProperties());
			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", returnDoc.getId());

		} catch (Exception e) {
			LOG.info("GetDocumentListService NEW REST API : Exception editDocumentTypes -- " + e);
			LOG.info("GetDocumentListService NEW REST API : Exception editDocumentTypes -- " + e.getStackTrace());
			LOG.info("GetDocumentListService NEW REST API : Exception editDocumentTypes -- " + e.getCause());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).build();
		} else {
			return Response.serverError().build();
		}
	}
	

	
	

}