package com.psl.applications;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import javax.xml.bind.DatatypeConverter;

import java.io.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.psl.beans.ApplicationConstants;
import com.psl.applications.IppService;

public class DocumentRepositoryServices {

	@Autowired
	IppService ippService;

	public IppService getIppService() {
		return ippService;
	}

	public void setIppService(IppService ippService) {
		this.ippService = ippService;
	}

	static final Logger LOG = LogManager.getLogger(DocumentRepositoryServices.class);

	/**
	 * This method will fetch document from Centera/Isilon and save the pdf/tiff
	 * files.
	 * 
	 * @param DmsId
	 * @param server
	 * @return
	 */
	public Map<String, byte[]> fetchAndSaveDocumentFromRepository(String DmsId, String server) {

		String fetchUri = server.trim() + ApplicationConstants.REPOSITORY_FETCH_URL.getValue().trim() + DmsId.trim();
		LOG.info("The URL for fetching the document : " + fetchUri);
		Map<String, byte[]> documentDetails = new HashMap<String, byte[]>();
		byte[] originalFileContent = null;
		URL fetch_url;
		try {
			fetch_url = new URL(fetchUri);
			HttpURLConnection connection;
			connection = (HttpURLConnection) fetch_url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			if (connection.getResponseCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Failed to Get the docuemnt for DmsId" + DmsId + " with HTTP Error code : "
						+ connection.getResponseCode() + " and Response Message : " + connection.getResponseMessage());
			}
			Scanner sc = new Scanner(fetch_url.openStream());
			String inline = "";
			while (sc.hasNext()) {
				inline += sc.nextLine();
			}
			if (inline != "") {
				sc.close();
				JsonObject jobj = ippService.parseJSONObject(inline);
				String fileNameExtnsn = jobj.get("file").getAsString();
				String[] fileNameArr = fileNameExtnsn.split("\\.");
				int nameLength = fileNameArr.length;
				String extension = fileNameArr[nameLength - 1];
				String fileName = fileNameExtnsn.substring(0, fileNameExtnsn.indexOf("." + extension));
				JsonElement fileData = jobj.get("fileData");
				inline = fileData.getAsString();
				originalFileContent = DatatypeConverter.parseBase64Binary(inline);
				if (!extension.equalsIgnoreCase("pdf")) {
					if (extension.equalsIgnoreCase("tif") || extension.equalsIgnoreCase("tiff")) {
						byte[] convertedContent = ippService.convertMultiStripTifToPDF(originalFileContent);
						documentDetails.put(fileName, convertedContent);

						String saveLocation = "\\\\ngd024972\\Deployment service my jar\\models\\Shared Location\\test-"
								+ fileName + ".pdf";
						LOG.info("Save Location = " + saveLocation);
						FileOutputStream fos = new FileOutputStream(new File(saveLocation));
						fos.write(convertedContent);
						fos.flush();
						fos.close();
					} else {
						throw new RuntimeException(
								"Invalid document format passed : Splitting and Merging is only supported for PDF and TIFF formats");
					}
				} else {
					documentDetails.put(fileName, originalFileContent);
				}
			} else {
				LOG.info("Error in Get Document Web Service");
			}

		} catch (IOException e) {
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e);
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getCause());
		} catch (DocumentException e) {
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e);
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getCause());
		}
		return documentDetails;
	}

	/**
	 * Merge and Split PDFs as per the specification for scanning & Indexing
	 * 
	 * @param documentDetails
	 * @param jsonObject
	 * @param server
	 * @return
	 */
	public Map<String, String> mergePdfDocuments(Map<String, byte[]> documentDetails, JsonObject jsonObject,
			String server) {
		Map<String, String> uploadedDocumentDetails = new HashMap<String, String>();
		JsonObject specificationObject = jsonObject.get("documentSplitter").getAsJsonObject();
		Set<Map.Entry<String, JsonElement>> jsonMap = specificationObject.entrySet();
		String documentType = "";
		String pageNumbersList = "";
		String filename = "";
		byte[] fileContent = null;
		PdfReader reader;
		PdfWriter pdfWriter = null;
		ByteArrayOutputStream outputStream = null;
		PdfImportedPage pdfImportedPage;
		PdfContentByte contentByte;
		String[] pageNumberArray;
		Rectangle rectangle;
		int currentPgNo;
		String dmsId = "";
		InputStream inputStream = null;
		try {
			for (Map.Entry<String, byte[]> documentEntry : documentDetails.entrySet()) {
				filename = documentEntry.getKey();
				LOG.info("fileName from map :" + filename);
				fileContent = documentEntry.getValue();
				inputStream = new ByteArrayInputStream(fileContent);
				LOG.info("InputStream count :" + inputStream.available());
				reader = new PdfReader(inputStream);

				for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {
					documentType = mapEntry.getKey().toString();
					pageNumbersList = mapEntry.getValue().getAsString();
					pageNumberArray = pageNumbersList.split(",");
					com.itextpdf.text.Document document = new com.itextpdf.text.Document();
					outputStream = new ByteArrayOutputStream();
					pdfWriter = PdfWriter.getInstance(document, outputStream);
					document.open();
					contentByte = pdfWriter.getDirectContent();
					for (String number : pageNumberArray) {
						currentPgNo = Integer.parseInt(number);
						rectangle = reader.getPageSize(currentPgNo);
						LOG.info("rectangle size : " + rectangle.toString());
						document.setPageSize(rectangle);
						document.newPage();
						pdfImportedPage = pdfWriter.getImportedPage(reader, currentPgNo);
						contentByte.addTemplate(pdfImportedPage, 0, 0);
					}
					document.close();
					LOG.info("OutputStream : " + outputStream.toByteArray().length);
					dmsId = uploadDocumentToRepository(outputStream.toByteArray(), filename, documentType, server,
							jsonObject);
					uploadedDocumentDetails.put(documentType, dmsId);
					outputStream.flush();
				}
				outputStream.close();
				pdfWriter.close();
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
		LOG.info("The Uploaded Details are : " + uploadedDocumentDetails);
		return uploadedDocumentDetails;
	}

	/**
	 * Upload the documents to the Repository with the keywords
	 * 
	 * @param fileContent
	 * @param fileName
	 * @param docType
	 * @param server
	 * @param jsonObject
	 * @return
	 */
	public String uploadDocumentToRepository(byte[] fileContent, String fileName, String docType, String server,
			JsonObject jsonObject) {
		String dmsId = "";
		JsonObject response = new JsonObject();
		JsonObject body = new JsonObject();
		JsonObject innerBody = new JsonObject();
		String memberNo = "";
		String schemeNo = "";
		String workType = "";
		Long processOid = 0L;
		String uploadedDate = "";
		String memberFirst = "";
		String memberLast = "";
		String schemeName = "";
		String comments = "";
		BufferedReader br = null;

		String fileDataString = DatatypeConverter.printBase64Binary(fileContent);
		String uploadUri = server.trim() + ApplicationConstants.REPOSITORY_UPLOAD_URL.getValue().trim();
		LOG.info("The URL for uploading the document : " + uploadUri);
		String completeFileName = fileName + "-" + docType + ".pdf";

		try {
			URL uploadUrl = new URL(uploadUri);
			HttpURLConnection addConnection;
			String system = server.split("/")[2].trim().split(":")[0];
			addConnection = (HttpURLConnection) uploadUrl.openConnection();
			addConnection.setRequestMethod("POST");
			addConnection.setRequestProperty("Content-Type", "application/json");
			addConnection.setDoOutput(true);
			body.addProperty("createdBy", "PSL Scanning & Indexing");
			body.addProperty("system", system);
			body.addProperty("fileData", fileDataString);
			body.addProperty("file", completeFileName);
			body.add("keywords", null);

			innerBody.addProperty("documentType", docType);

			if (jsonObject.get("uploadedDate") != null) {
				uploadedDate = jsonObject.get("uploadedDate").getAsString();
				innerBody.addProperty("uploadedDate", uploadedDate);
			}
			if (jsonObject.get("memberFirstName") != null) {
				memberFirst = jsonObject.get("memberFirstName").getAsString();
				innerBody.addProperty("memberFirstName", memberFirst);
			}
			if (jsonObject.get("memberLastName") != null) {
				memberLast = jsonObject.get("memberLastName").getAsString();
				innerBody.addProperty("memberLastName", memberLast);
			}
			if (jsonObject.get("memberNo") != null) {
				memberNo = jsonObject.get("memberNo").getAsString();
				innerBody.addProperty("memberNo", memberNo);
			}
			if (jsonObject.get("schemeName") != null) {
				schemeName = jsonObject.get("schemeName").getAsString();
				innerBody.addProperty("schemeName", schemeName);
			}
			if (jsonObject.get("schemeNo") != null) {
				schemeNo = jsonObject.get("schemeNo").getAsString();
				innerBody.addProperty("schemeNo", schemeNo);
			}
			if (jsonObject.get("workType") != null) {
				workType = jsonObject.get("workType").getAsString();
				innerBody.addProperty("workType", workType);
			}
			if (jsonObject.get("processOID") != null) {
				processOid = jsonObject.get("processOID").getAsLong();
				innerBody.addProperty("processOID", processOid);
			}
			if (jsonObject.get("comments") != null) {
				comments = jsonObject.get("comments").getAsString();
				innerBody.addProperty("comments", comments);
			}

			body.add("keywords2", innerBody);

			String input = body.toString();
			LOG.info("upload file json : " + input);
			OutputStream os = addConnection.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			if (addConnection.getResponseCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Failed to Upload Document " + completeFileName
						+ " to Repository with HTTP error code : " + addConnection.getResponseCode()
						+ " and reponse message : " + addConnection.getResponseMessage());
			}

			jsonObject = null;
			br = new BufferedReader(new InputStreamReader((addConnection.getInputStream())));
			dmsId = "";
			LOG.info("Output from Server .... \n");
			while ((dmsId = br.readLine()) != null) {
				LOG.info(dmsId);
				if (dmsId.matches("[0-9]+")) {
					break;
				}
			}
		} catch (IOException e) {
			LOG.info("Uploading Docuemnts  : Exception UploadDocuments -- " + e);
			LOG.info("Uploading Docuemnts : Exception UploadDocuments -- " + e.getStackTrace());
			LOG.info("Uploading Docuemnts : Exception UploadDocuments -- " + e.getCause());

		}

		return dmsId;
	}

}
