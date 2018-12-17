package com.psl.services;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.io.*;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.psl.applications.DocumentRepositoryServices;
import com.psl.applications.IppService;
import com.psl.beans.ApplicationConstants;

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

	@Autowired
	DocumentRepositoryServices documentRepositoryServices;

	private static final Logger LOG = LogManager.getLogger(DocumentServices.class);

	/**
	 * Fetches the documents attached to process instance, oid passed
	 * 
	 * @param processOid
	 *            OID of the process instance
	 * @return attached documents
	 */
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

				Collections.sort(docsList, new Comparator<Document>() {
					public int compare(Document d1, Document d2) {
						return d2.getDateCreated().compareTo(d1.getDateCreated());
					}
				});

			} catch (Exception e) {
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess -- " + e);
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e.getStackTrace());
				LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e.getCause());
				return Response.ok("There are No Documents Found!").build();
			}

			return ippService.prepareJSONData(docsList);

		} catch (Exception e) {
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e);
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e.getStackTrace());
			LOG.info("getDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e.getCause());
			return Response.ok("No Documents Found. Some Exceptions!").build();
		}
	}

	/**
	 * This method returns a list of documents in a paginated way based on size
	 * and page number on the UI. Also, a search parameter can be passed to
	 * return documents matching the search string.
	 * 
	 * @param pageSize
	 *            - 5 or 10 records
	 * @param pageNumber
	 *            - page number on the UI
	 * @param poId
	 *            - process instance OId
	 * @param searchParam
	 *            - Document name or document type
	 * @return documents List in JSON
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getPaginatedDocuments")
	public Response getPaginatedDocuments(@QueryParam("pageSize") short pageSize,
			@QueryParam("pageNumber") short pageNumber, @QueryParam("poId") long poId,
			@QueryParam("searchParam") String searchParam) {
		try {

			if ((pageSize <= 0) || (pageNumber <= 0)) {
				LOG.info(
						"getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess -- Invalid page size/number");
				return Response.ok("Invalid page size/number. Some Exceptions!").build();
			}

			LOG.info("getPaginatedDocuments REST API !");
			List<Document> docsListTemp;
			List<Document> docsList = new ArrayList<Document>();

			try {

				docsListTemp = ippService.getAttachedDocuments(poId);

				for (Document doc : docsListTemp) {
					if (searchParam != null) {
						if ((doc.getName() != null)
								&& (doc.getName().toLowerCase().contains(searchParam.toLowerCase()))) {
							docsList.add(doc);
						} else if (doc.getProperty("DocumentType") != null) {
							List<String> docTypes = (List) doc.getProperty("DocumentType");

							String docTypesStr = StringUtils.join(docTypes.iterator(), ",");
							if (docTypesStr.toLowerCase().contains(searchParam.toLowerCase())) {
								docsList.add(doc);
							}
						}
					} else {
						docsList.add(doc);
					}
				}
				int totalCount = docsList.size();

				int fromIndex = (pageNumber - 1) * pageSize;
				if ((docsList == null) || (docsList.size() < fromIndex)) {
					fromIndex = 0;
				}

				Collections.sort(docsList, new Comparator<Document>() {
					public int compare(Document d1, Document d2) {
						return d2.getDateCreated().compareTo(d1.getDateCreated());
					}
				});

				List<Document> paginatedDocsList = docsList.subList(fromIndex,
						Math.min(fromIndex + pageSize, docsList.size()));

				return ippService.preparePaginatedJSONData(paginatedDocsList, totalCount, docsListTemp);

			} catch (Exception e) {
				LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess -- " + e);
				LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
						+ e.getStackTrace());
				LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
						+ e.getCause());
				return Response.ok("There are No Documents Found!").build();
			}

			/*
			 * return Response.ok("", "application/json") .header("Expires",
			 * "-1").header("Cache-Control", "must-revalidate, private"
			 * ).build();
			 */

		} catch (Exception e) {
			LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e);
			LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- "
					+ e.getStackTrace());
			LOG.info("getPaginatedDocuments REST API : Exception getProcessDocumentsIPPForAProcess-- " + e.getCause());
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

	/**
	 * 
	 * @param input
	 * @return
	 */
	@POST
	@Path("editDocumentTypes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response editDocumentTypes(String input) {

		LOG.info("editDocumentTypes REST API : Started Edit Document Types !");

		JsonObject response = null;
		String memberNo = null, schemeNo = null, workType = null;
		JsonObject jsonObject = ippService.parseJSONObject(input);
		String docId = jsonObject.get(ApplicationConstants.DOC_ID.getValue()).getAsString();

		Document document = ippService.getDocumentManagementService().getDocument(docId);
		Map<String, Object> existingProperties = document.getProperties();
		try {
			memberNo = jsonObject.get("memberNo").getAsString();
		} catch (Exception e) {
			try {
				memberNo = (String) existingProperties.get(ApplicationConstants.META_DATA_MEMBER_NO.getValue());
			} catch (Exception e1) {
				memberNo = null;
			}
		}
		try {
			schemeNo = jsonObject.get("schemeNo").getAsString();
		} catch (Exception e) {
			try {
				schemeNo = (String) existingProperties.get(ApplicationConstants.META_DATA_SCHEME_NO.getValue());
			} catch (Exception e1) {
				schemeNo = null;
			}
		}
		String processOid = jsonObject.get("processOid").getAsString();
		String docTypes = jsonObject.get("docTypes").getAsString();
		try {
			workType = jsonObject.get("workType").getAsString();
		} catch (Exception e) {
			try {
				workType = (String) existingProperties.get(ApplicationConstants.META_DATA_REQUEST_TYPE.getValue());
			} catch (Exception e1) {
				workType = null;
			}
		}

		try {

			Map<String, Object> metaDataMap = ippService.populateDocumentProperties(schemeNo, memberNo, workType,
					docTypes);

			LOG.info("editDocumentTypes REST API : Document Properties : " + metaDataMap);

			Document returnDoc = ippService.updateNonSecureDocument(docId, Long.parseLong(processOid), metaDataMap);

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
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * Fetches the content of the document based on the JCR ID passed
	 * 
	 * @param input
	 *            JCR ID
	 * @return File Content (File Name and Byte Content)
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("getDocumentContent")
	public Response getDocumentContent(String input) {

		JsonObject jsonObject = null;
		String response = null;

		try {
			jsonObject = ippService.parseJSONObject(input);
			String docId = jsonObject.get(ApplicationConstants.DOC_ID.getValue()).getAsString();
			LOG.info("getDocumentContent  REST API : Get document content for a documentID !");

			try {
				response = ippService.getDocumentContent(docId);

			} catch (Exception e) {
				LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 1 -- " + e);
				LOG.info("getDocumentContent  REST API: Exception getDocumentContent  REST API 1-- "
						+ e.getStackTrace());
				LOG.info("getDocumentContent  REST API: Exception getDocumentContent  REST API 1-- " + e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).header("Access-Control-Allow-Origin", "*")
					.build();

		} catch (Exception e) {
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2 -- " + e);
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2-- " + e.getStackTrace());
			LOG.info("getDocumentContent  REST API : Exception getDocumentContent  REST API 2-- " + e.getCause());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Uploads document against the Process OID passed
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
	@POST
	@Path("addDocument")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addFiles(@Multipart("file") Attachment attachment, @Context HttpServletRequest request,
			@Multipart("processOid") String processOid, @Multipart("docTypes") String docTypes,
			@Multipart("memberNo") String memberNo, @Multipart("schemeNo") String schemeNo,
			@Multipart("workType") String workType) {

		LOG.info("addFiles REST API : Started Upload Files using CXF !");

		JsonObject response = null;

		try {

			Map<String, Object> metaDataMap = ippService.populateDocumentProperties(schemeNo, memberNo, workType,
					docTypes);
			LOG.info("addDocumentTypes REST API : Document Properties : " + metaDataMap);

			DataHandler dataHandler = attachment.getDataHandler();
			InputStream inputStream = dataHandler.getInputStream();

			MultivaluedMap<String, String> headers = attachment.getHeaders();
			FileInfo fileInfo = getFileName(headers);

			byte[] documentContent = IOUtils.toByteArray(inputStream);
			LOG.debug("Process OID :" + processOid + "docTypes :" + docTypes + "memberNo:+" + memberNo + "schemeNo:"
					+ schemeNo + "ADD DOCUMENTS REST API : byte[] : " + Arrays.toString(documentContent));
			String contentType = URLConnection.guessContentTypeFromName(fileInfo.name);

			Document document = ippService.saveDocumentinIpp(documentContent, fileInfo.name, contentType,
					Long.parseLong(processOid), metaDataMap);

			inputStream.close();

			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", document.getId());
			response.addProperty("DocumentTypes", ippService.getDocumentTypeString(document));

		} catch (Exception e) {
			LOG.info("addDocuments NEW REST API : Exception addFiles -- " + e.getMessage());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * Removes document based on the JCR ID and Process ID passed
	 * 
	 * @param input
	 *            Process OID and JCR ID of the document to be removed
	 * @return
	 */
	@POST
	@Path("removeDocument")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeDocument(String input) {

		LOG.info("Remove Document REST API : Started Remove Document !");

		JsonObject response = null;

		JsonObject jsonObject = ippService.parseJSONObject(input);
		String docId = jsonObject.get(ApplicationConstants.DOC_ID.getValue()).getAsString();
		long processOid = jsonObject.get("processOid").getAsLong();
		try {

			ippService.removeDocument(docId, processOid);
			response = new JsonObject();
			response.addProperty("Success", true);
			response.addProperty("DocumentId", docId);

		} catch (Exception e) {
			LOG.info("Remove Document REST API : Exception removeDocument -- " + e.getStackTrace());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * Service to attach documents from one process instance to another process
	 * instance
	 * 
	 * @param String
	 *            Stringified JSON, Process OID and JCR ID of the document to be
	 *            removed
	 * @return
	 */
	@POST
	@Path("attachDocumentsToProcess")
	@Produces(MediaType.APPLICATION_JSON)
	public Response attachDocumentsToProcess(String input) {

		LOG.info("Attach Document REST API : Started Attaching Document !");

		JsonObject response = null;

		JsonObject jsonObject = ippService.parseJSONObject(input);
		long fromProcessOid = jsonObject.get("fromProcessOid").getAsLong();
		long toProcessOid = jsonObject.get("toProcessOid").getAsLong();
		try {

			ippService.attachDocuments(fromProcessOid, toProcessOid);
			response = new JsonObject();
			response.addProperty("Success", true);

		} catch (Exception e) {
			LOG.info("Attach Document REST API : Exception AttachDocument -- " + e.getStackTrace());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
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
	@POST
	@Path("uploadDocument")
	@Consumes({ "multipart/form-data" })
	@Produces({ "application/json" })
	public Response uploadFilesInFolder(@Multipart("file") Attachment attachment, @Context HttpServletRequest request,
			@Multipart("docTypes") String docTypes, @Multipart("memberNo") String memberNo,
			@Multipart("schemeNo") String schemeNo, @Multipart("workType") String workType) {
		LOG.info("addFiles REST API : Started Upload Files using CXF !");

		JsonObject response = null;

		try {
			Map<String, Object> metaDataMap = this.ippService.populateDocumentProperties(schemeNo, memberNo, workType,
					docTypes);
			LOG.info("uploadFilesInFolder REST API : Document Properties : " + metaDataMap);

			DataHandler dataHandler = attachment.getDataHandler();
			InputStream inputStream = dataHandler.getInputStream();

			MultivaluedMap<String, String> headers = attachment.getHeaders();
			FileInfo fileInfo = getFileName(headers);

			byte[] documentContent = IOUtils.toByteArray(inputStream);
			LOG.debug("docTypes :" + docTypes + "memberNo:+" + memberNo + "schemeNo:" + schemeNo
					+ "ADD DOCUMENTS REST API : byte[] : " + Arrays.toString(documentContent));
			String contentType = URLConnection.guessContentTypeFromName(fileInfo.name);

			Document document = this.ippService.uploadDocinFolder(documentContent, fileInfo.name, contentType,
					metaDataMap);

			inputStream.close();

			response = new JsonObject();
			response.addProperty("name", document.getName());
			response.addProperty("creationTimestamp", document.getDateCreated().getTime());
			response.addProperty("contentType", document.getContentType());
			response.addProperty("path", document.getPath());
			response.addProperty("uuid", document.getId());
			response.addProperty("numPages", 0);
			JsonObject docType = new JsonObject();
			docType.addProperty("name", docTypes);
			response.add("documentType", docType);
			response.addProperty("pageCount", 0);
			response.addProperty("url", ApplicationConstants.URL_STRING.getValue() + document.getId());
			response.addProperty("pages", "[]");
		} catch (Exception e) {
			LOG.info("uploadFilesInFolder NEW REST API : Exception addFiles -- " + e.getMessage());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * Delete the document based on the JCR ID passed (Not used)
	 * 
	 * @param String
	 *            input Stringified JSON consists of JCR ID
	 * @return
	 */
	@POST
	@Path("deleteDocument")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteDocument(String input) {

		LOG.info("Remove Document REST API : Started Remove Document !");

		JsonObject response = null;

		JsonObject jsonObject = ippService.parseJSONObject(input);
		String docId = jsonObject.get(ApplicationConstants.DOC_ID.getValue()).getAsString();
		try {

			ippService.deleteDocuments(docId);
			response = new JsonObject();
			response.addProperty("Success", true);

		} catch (Exception e) {
			LOG.info("Delete Document REST API : Exception removeDocument -- " + e.getStackTrace());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * Deprecated This is the web service which utilises the methods of IPP
	 * service class which cater for single strip tiff documents only
	 * 
	 * @param processOid
	 * @return
	 */
	@PUT
	@Path("process-instance/{oid: \\d+}/convertDocuments")
	@Produces(MediaType.APPLICATION_JSON)
	public Response convertDocuments(@PathParam("oid") String processOid) {

		LOG.info("Convert Document REST API : Started converting the Documents !");

		long pOid = Long.parseLong(processOid);

		JsonObject response = null;
		try {

			// List<Document> docsList = ippService.getAttachedDocuments(pOid);

			ippService.convertDocumentTIFtoPDF(pOid);

			response = new JsonObject();
			response.addProperty("Success", true);

		} catch (Exception e) {
			LOG.error("Convert Document REST API : Exception convertDocuments -- " + e.getStackTrace());
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * This is the web service which utilises the methods of IPP service class
	 * which cater for single as well as multi strip tiff document conversion to
	 * PDF
	 * 
	 * @param attachment
	 * @param Path
	 * @return
	 */
	@POST
	@Path("convertDocBatch")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response convertDocBatch(@Multipart("file") Attachment attachment, @Multipart("path") String Path) {

		LOG.info("Inside convertDocBatch REST API Method -- File received");
		JsonObject response = null;
		DataHandler dataHandler = attachment.getDataHandler();
		InputStream inputStream = null;

		try {
			inputStream = dataHandler.getInputStream();
			List<String> documentContentLines = IOUtils.readLines(inputStream);
			int length = documentContentLines.size();
			if (length > 0) {
				Properties properties = ippService.readExternalPropertiesFile(Path);
				int counter = Integer.parseInt(properties.getProperty("counter"));
				int batchSize = Integer.parseInt(properties.getProperty("batch"));
				int checkpoint = Integer.parseInt(properties.getProperty("checkpoint"));
				LOG.info("Read Properties file, details are: Counter = " + counter + "\n Batch size = " + batchSize
						+ "\n Checkpoint = " + checkpoint);

				int counterLimit = counter + batchSize;
				if (length < counterLimit) {
					counterLimit = length;
				}
				// Looping the document ID's from counter till batch size read
				// from properties file
				int checkpointLimit;
				for (int i = counter; i < counterLimit;) {

					checkpointLimit = checkpoint;
					if (counterLimit < (checkpointLimit + i)) {
						checkpointLimit = counterLimit - i;
					}
					List<String> documentIds = new ArrayList<String>();
					// Adding the checkpoint number of documentIDs to arraylist
					// for converting the document
					for (int j = 0; j < checkpointLimit; j++, i++) {
						documentIds.add(documentContentLines.get(i));
					}
					LOG.info(
							"Inside convertDocBatch REST API Method -- Calling convertDocTiffToPDFBatch method and passimg the doc IDs list");
					ippService.convertDocTiffToPDFBatch(documentIds);

					LOG.info("Inside convertDocBatch REST API Method -- Updating the properties file with counter as :"
							+ i);
					properties.setProperty("counter", Integer.toString(i));
					ippService.writeExternalPropertiesFile(properties, Path);

				}
			} else {
				LOG.warn("No documents to convert from tiff to PDF");
			}

			response = new JsonObject();
			response.addProperty("Success", true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.error("Exception in Reading file :" + e.getCause());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * This is the web service which utilises the methods of IPP service class
	 * which assigns docType to documents not having docType assigned to them
	 * 
	 * @param attachment
	 * @param Path
	 * @return
	 */
	@POST
	@Path("assignDoctype")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response assignDoctype(@Multipart("file") Attachment attachment, @Multipart("path") String Path) {
		LOG.info("Assign DocType to documents : Started");
		JsonObject response = null;
		DataHandler dataHandler = attachment.getDataHandler();
		InputStream inputStream;

		try {
			inputStream = dataHandler.getInputStream();
			List<String> documentContentLines = IOUtils.readLines(inputStream);
			int length = documentContentLines.size();
			if (length > 0) {
				Properties properties = ippService.readExternalPropertiesFile(Path);
				int counter = Integer.parseInt(properties.getProperty("counter"));
				int batchSize = Integer.parseInt(properties.getProperty("batch"));
				int checkpoint = Integer.parseInt(properties.getProperty("checkpoint"));
				LOG.info("Counter = " + counter + "\n Batch size = " + batchSize + "\n Checkpoint = " + checkpoint);

				int counterLimit = counter + batchSize;
				if (length < counterLimit) {
					counterLimit = length;
				}
				// Looping the document ID's from counter till batch size read
				// from properties file
				for (int i = counter; i < counterLimit;) {

					int checkpointLimit = checkpoint;
					if (counterLimit < (checkpointLimit + i)) {
						checkpointLimit = counterLimit - i;
					}
					List<String> documentIds = new ArrayList<String>();
					// Adding the checkpoint number of documentIDs to arraylist
					// for updating the docType
					for (int j = 0; j < checkpointLimit; j++, i++) {
						LOG.info("jcrId = " + documentContentLines.get(i));
						documentIds.add(documentContentLines.get(i).split(",")[0]);
					}
					ippService.doctypeAssignment(documentIds);

					properties.setProperty("counter", Integer.toString(i));
					ippService.writeExternalPropertiesFile(properties, Path);

				}
			} else {
				LOG.info("No documents to assign DocType");
			}

			response = new JsonObject();
			response.addProperty("Success", true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.error("Exception in Reading file :" + e.getCause());
			e.printStackTrace();
		}

		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * The service performs editing of multiple documents in one call!
	 * 
	 * @param input
	 * @return
	 */
	@POST
	@Path("editMultiDocumentsType")
	@Produces(MediaType.APPLICATION_JSON)
	public Response editMultiDocumentsType(String input) {

		LOG.info("editMultiDocumentsType REST API : Started Edit Documents !");

		JsonArray mainJsonArray = new JsonArray();
		JsonObject response = null;
		String memberNo = null, schemeNo = null, workType = null;
		try {
			JsonObject jsonObject = ippService.parseJSONObject(input);
			JsonArray jsonArray = jsonObject.getAsJsonArray("documentList");
			for (JsonElement jsonElement : jsonArray) {
				try {
					response = new JsonObject();
					JsonObject jObject = jsonElement.getAsJsonObject();
					String docId = jObject.get(ApplicationConstants.DOC_ID.getValue()).getAsString();

					response.addProperty("DocumentId", docId);

					Document document = ippService.getDocumentManagementService().getDocument(docId);
					Map<String, Object> existingProperties = document.getProperties();
					try {
						memberNo = jObject.get("memberNo").getAsString();
					} catch (Exception e) {
						try {
							memberNo = (String) existingProperties
									.get(ApplicationConstants.META_DATA_MEMBER_NO.getValue());
						} catch (Exception e1) {
							memberNo = null;
						}
					}
					try {
						schemeNo = jObject.get("schemeNo").getAsString();
					} catch (Exception e) {
						try {
							schemeNo = (String) existingProperties
									.get(ApplicationConstants.META_DATA_SCHEME_NO.getValue());
						} catch (Exception e1) {
							schemeNo = null;
						}
					}
					try {
						workType = jObject.get("workType").getAsString();
					} catch (Exception e) {
						try {
							workType = (String) existingProperties
									.get(ApplicationConstants.META_DATA_REQUEST_TYPE.getValue());
						} catch (Exception e1) {
							workType = null;
						}
					}

					String processOid = jObject.get("processOid").getAsString();
					String docTypes = jObject.get("docTypes").getAsString();

					Map<String, Object> metaDataMap = ippService.populateDocumentProperties(schemeNo, memberNo,
							workType, docTypes);

					LOG.info("editMultiDocumentsType REST API : Document Properties : " + metaDataMap);

					Document returnDoc = ippService.updateNonSecureDocument(docId, Long.parseLong(processOid),
							metaDataMap);

					LOG.info("editMultiDocumentsType REST API : Returned Doc- Doc name : " + returnDoc.getName());
					LOG.info("editMultiDocumentsType REST API : Returned Doc - docId: " + returnDoc.getId());
					LOG.info("editMultiDocumentsType REST API : Returned Doc - properties : "
							+ returnDoc.getProperties());

					response.addProperty("Success", true);

				} catch (Exception e) {
					response.addProperty("Success", false);
					LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- " + e);
					LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- "
							+ e.getStackTrace());
					LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- " + e.getCause());
					e.printStackTrace();
				}
				mainJsonArray.add(response);
			}

		} catch (Exception e) {
			LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- " + e);
			LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- " + e.getStackTrace());
			LOG.info("editMultiDocumentsType REST API : Exception editMultiDocumentsType -- " + e.getCause());
			e.printStackTrace();
		}

		if (mainJsonArray.size() > 0) {
			return Response.ok(mainJsonArray.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}

	/**
	 * This method will download, split, merge and upload the splitted documents
	 * to centera as PDF documents
	 * 
	 * @param input
	 * @return
	 */

	@POST
	@Path("splitAndMerge")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response splitAndMergePdfDocument(String input) {
		LOG.info("Splitting and Merging documents : Started " + input);
		JsonObject jsonObject = ippService.parseJSONObject(input);
		JsonArray documentSplitterArray = null;
		JsonObject response = new JsonObject();
	
		if (jsonObject.get("documentSplitter") != null) {
			documentSplitterArray = jsonObject.get("documentSplitter").getAsJsonArray();
			jsonObject.remove("documentSplitter");
		} else {
			return Response.status(400)
					.entity("{\"response\":\"'documentSplitter' parameter missing in request body.\"}").build();
		}
		try {
			LinkedHashMap<String,LinkedHashMap<byte[],int[]>> formattedSpecificationMap = ippService.populateDataForSplitMerge(documentSplitterArray);
			
			LinkedHashMap<String,String> uploadDetails = ippService.mergePdfDocuments(formattedSpecificationMap,jsonObject);
			LOG.info(uploadDetails.toString());
			for (Map.Entry<String, String> detailsEntry : uploadDetails.entrySet()) {
				response.addProperty(detailsEntry.getKey(), detailsEntry.getValue());
			}

		} catch (Exception e) {
			LOG.info("Split and Merge Document : Exception -- " + e);
			LOG.info("Split and Merge Document : Exception -- " + e.getStackTrace().toString());
			LOG.info("Split and Merge Document : Exception -- " + e.getCause());
		}
		if (response != null) {
			return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE)
					.header("Access-Control-Allow-Origin", "*").build();
		} else {
			return Response.serverError().build();
		}
	}
	
}
