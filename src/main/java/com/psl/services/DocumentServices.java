package com.psl.services;


import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.JsonObject;
import com.psl.applications.IppService;

@Component
@Path("/document-services")
public class DocumentServices {



	@Autowired
	IppService ippService;



	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	private static final Logger LOG = LogManager.getLogger(DocumentServices.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("process-instance/{oid: \\d+}/getDocuments")
	public Response getProcessDocuments(@PathParam("oid") String processOid) {
		try {

			LOG.info("getDocuments REST API : Get documents For A Process !");
			List<Document> docsList = new ArrayList<Document>();

			Long processOID = 0L;

			try {
				processOID = Long.parseLong(processOid);

				docsList = ippService.getAttachedDocuments(processOID);

			} catch (Exception e) {
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess -- " + e);
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
						+ e.getStackTrace());
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
						+ e.getCause());
				e.printStackTrace();
				return Response.ok("There are No Documents Found!").build();
			}

			return ippService.prepareJSONData(docsList);

		} catch (Exception e) {
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e);
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
					+ e.getStackTrace());
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
					+ e.getCause());
			e.printStackTrace();
			return Response.ok("No Documents Found. Some Exceptions!").build();
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
	@Path("editDocumentTypes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response editDocumentTypes(String input) {


		LOG.info("editDocumentTypes REST API : Started Edit Document Types !");

		JsonObject response = null;

		JsonObject jsonObject = ippService.parseJSONObject(input);
		String docId = jsonObject.get("docId").getAsString();
		String memberNo = jsonObject.get("memberNo").getAsString();
		String schemeNo = jsonObject.get("schemeNo").getAsString();
		String processOid = jsonObject.get("processOid").getAsString();
		String docTypes = jsonObject.get("docTypes").getAsString();
		String workType = jsonObject.get("workType").getAsString();


		try {

			Map<String, Object> metaDataMap = ippService.populateDocumentProperties(schemeNo, memberNo, workType, docTypes);

			LOG.info("editDocumentTypes REST API : Document Properties : " + metaDataMap);

			Document returnDoc = ippService.updateNonSecureDocument(docId,Long.parseLong(processOid), metaDataMap);

			LOG.info("editDocumentTypes REST API : Returned Doc- Doc name : " + returnDoc.getName());
			LOG.info("editDocumentTypes REST API : Returned Doc - docId: " + returnDoc.getId());
			LOG.info("editDocumentTypes REST API : Returned Doc - properties : " + returnDoc.getProperties());

			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", returnDoc.getId());

		} catch (Exception e) {
			LOG.info("editDocumentTypes REST API : Exception editDocumentTypes -- " + e);
			LOG.info("editDocumentTypes REST API : Exception editDocumentTypes -- " + e.getStackTrace());
			LOG.info("editDocumentTypes REST API : Exception editDocumentTypes -- " + e.getCause());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}



	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("getDocumentContent")
	public Response getDocumentContent(String input) {
		
		JsonObject jsonObject = null;
		String response = null;
	
		try {
			jsonObject = ippService.parseJSONObject(input);
			String docId = jsonObject.get("docId").getAsString();
			LOG.info("getDocumentContent  REST API : Get document content for a documentID !");
			
			try {
				response = ippService.getDocumentContent(docId);
				
			} catch (Exception e) {
				LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 1 -- " + e);
				LOG.info("getDocumentContent  REST API: Exception getDocumentContent  REST API 1-- "
						+ e.getStackTrace());
				LOG.info("getDocumentContent  REST API: Exception getDocumentContent  REST API 1-- "
						+ e.getCause());
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).header("Access-Control-Allow-Origin", "*").build();

		} catch (Exception e) {
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2 -- " + e);
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2-- "
					+ e.getStackTrace());
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2-- "
					+ e.getCause());
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	



	@POST
	@Path("addDocument")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addFiles(@Multipart("file")Attachment attachment,@Context HttpServletRequest request,@Multipart("processOid") String processOid
			,@Multipart("docTypes") String docTypes,@Multipart("memberNo") String memberNo,@Multipart("schemeNo") String schemeNo,@Multipart("workType") String workType){

		LOG.info("addFiles REST API : Started Upload Files using CXF !");


		JsonObject response = null;

		try {

			Map<String, Object> metaDataMap = ippService.populateDocumentProperties(schemeNo, memberNo, workType, docTypes);
			LOG.info("addDocumentTypes REST API : Document Properties : " + metaDataMap);

			DataHandler dataHandler = attachment.getDataHandler();
			InputStream inputStream = dataHandler.getInputStream();

			MultivaluedMap<String, String> headers = attachment.getHeaders();
			FileInfo fileInfo = getFileName(headers);

			byte[] documentContent = IOUtils.toByteArray(inputStream);
			LOG.debug("Process OID :"+processOid+"docTypes :"+docTypes+"memberNo:+"+memberNo+"schemeNo:"+schemeNo+"ADD DOCUMENTS REST API : byte[] : " + Arrays.toString(documentContent));
			String contentType = URLConnection.guessContentTypeFromName(fileInfo.name);

			Document document = ippService.saveDocumentinIpp(documentContent, fileInfo.name, contentType,Long.parseLong(processOid), metaDataMap);

			inputStream.close();

			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", document.getId());
			response.addProperty("DocumentTypes", ippService.getDocumentTypeString(document));

		} catch (Exception e) {
			LOG.info("addDocuments NEW REST API : Exception addFiles -- " + e.getMessage());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}




}