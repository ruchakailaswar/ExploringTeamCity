package com.psl.applications;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.io.*;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
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
	 * This method will fetch from Centera/Isilon and save the pdf document to
	 * the given location
	 * 
	 * @param DmsId
	 * @param basePath
	 * @param server
	 * @return
	 */
	public String fetchAndSaveDocumentFromRepository(String DmsId, String basePath, String server) {
		String saveLocation = basePath.trim();
		String fetchUri = server.trim() + ApplicationConstants.REPOSITORY_FETCH_URL.getValue().trim() + DmsId.trim();
		LOG.info("The URL for fetching the document : " + fetchUri);

		URL fetch_url;
		try {
			fetch_url = new URL(fetchUri);
			HttpURLConnection connection;
			connection = (HttpURLConnection) fetch_url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			if (connection.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP Error code : " + connection.getResponseCode());
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

				if (extension.equalsIgnoreCase("pdf")) {
					saveLocation = saveLocation + "\\" + DmsId + "-" + fileName + ".pdf";
					LOG.info("Save Location = " + saveLocation);
					FileOutputStream fos = new FileOutputStream(new File(saveLocation));
					fos.write(Base64.decodeBase64(inline));
					fos.flush();
					fos.close();
				} else {
					String tmpImgLocation = saveLocation + "\\" + DmsId + "-" + fileNameExtnsn;
					FileOutputStream fos = new FileOutputStream(new File(tmpImgLocation));
					fos.write(Base64.decodeBase64(inline));
					fos.flush();
					fos.close();
					saveLocation = convertImgToPdf(tmpImgLocation, DmsId, extension);
					LOG.info("Save Location = " + saveLocation);
				}

			} else {
				LOG.info("Error in Get Document Web Service");
			}
		} catch (IOException e) {
			LOG.info("Fetch and Save Document  : Exception convertDocuments -- " + e);
			LOG.info("Fetch and Save Document  : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Fetch and Save Document : Exception convertDocuments -- " + e.getCause());
		}

		return saveLocation;
	}

	/**
	 * convert images to pdf and store at a temporary location
	 * 
	 * @param location
	 * @param dmsId
	 * @param extension
	 * @return
	 */
	public String convertImgToPdf(String location, String dmsId, String extension) {
		String pdfLocation = location.substring(0, location.indexOf("." + extension)) + "-" + dmsId + ".pdf";
		try {
			if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")
					|| extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("gif")) {
				com.itextpdf.text.Document document = new com.itextpdf.text.Document();
				FileOutputStream fos;
				PdfWriter writer = null;
				fos = new FileOutputStream(pdfLocation);
				writer = PdfWriter.getInstance(document, fos);
				writer.open();
				document.open();
				document.add(Image.getInstance((new URL("file:///" + location))));
				document.close();
				writer.close();
				fos.close();
			} else {
				if (extension.equalsIgnoreCase("tif") || extension.equalsIgnoreCase("tiff")) {
					File tiffFile = new File(location);
					int length = (int) tiffFile.length();
					byte[] bytes = new byte[length];
					DataInputStream input = new DataInputStream(new FileInputStream(tiffFile));
					input.readFully(bytes);
					byte[] convertedContent = ippService.convertMultiStripTifToPDF(bytes);
					LOG.info("Save Location = " + pdfLocation);
					FileOutputStream fos1 = new FileOutputStream(new File(pdfLocation));
					fos1.write(convertedContent);
					fos1.close();
					input.close();
				}

			}
			File file = new File(location);
			if (file.delete()) {
				LOG.info(location + " is deleted");
			} else {
				LOG.info("Couldn't delete file " + location);
			}
		} catch (DocumentException e) {

			LOG.info("Convert Img to Pdf  : Exception convertDocuments -- " + e);
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getCause());

		} catch (IOException e) {
			LOG.info("Convert Img to Pdf  : Exception convertDocuments -- " + e);
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getStackTrace());
			LOG.info("Convert Img to Pdf : Exception convertDocuments -- " + e.getCause());

		}

		return pdfLocation;
	}

	/**
	 * Matching documents splitting specification as per the assigned
	 * DocumentType
	 * 
	 * @param fileLocation
	 * @param specificationObject
	 * @param dmsArr
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, String>> formatSplittingSpecification(
			ArrayList<String> fileLocation, JsonObject specificationObject, String[] dmsArr) {
		LinkedHashMap<String, LinkedHashMap<String, String>> outerMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		String documentType ="";
		String pageNumbersList = "";
		String dmsId ="";
		String filename = "";
		String docType = "";
		String pgNo = "";
		for (String dmsid : dmsArr) {
			LinkedHashMap<String, String> internalMap = new LinkedHashMap<String, String>();
			JsonObject jObj = specificationObject.get(dmsid).getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> jsonMap = jObj.entrySet();
			documentType = null;
			pageNumbersList = null;
			for (Map.Entry<String, JsonElement> mapEntry : jsonMap) {
				documentType = mapEntry.getKey().toString();
				pageNumbersList = mapEntry.getValue().getAsString();
				internalMap.put(documentType, pageNumbersList);
			}
			outerMap.put(dmsid, internalMap);
		}

		LinkedHashMap<String, LinkedHashMap<String, String>> docTypeMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		for (Map.Entry<String, LinkedHashMap<String, String>> outerEntry : outerMap.entrySet()) {
			dmsId = outerEntry.getKey();
			filename = "";
			for (String fLocation : fileLocation) {
				if (fLocation.contains(dmsId)) {
					filename = fLocation;
					break;
				}
			}
			for (Map.Entry<String, String> innerEntry : outerEntry.getValue().entrySet()) {
				docType = innerEntry.getKey();
				pgNo = innerEntry.getValue();
				if (docTypeMap.containsKey(docType)) {
					LinkedHashMap<String, String> existingMap = docTypeMap.get(docType);
					existingMap.put(filename, pgNo);
					docTypeMap.put(docType, existingMap);
				} else {
					LinkedHashMap<String, String> dmsPageNo = new LinkedHashMap<String, String>();
					dmsPageNo.put(filename, pgNo);
					docTypeMap.put(docType, dmsPageNo);

				}

			}
		}

		return docTypeMap;
	}

	/**
	 * Merge the Pdf's based on the jsonObject Specification
	 * 
	 * @param specificationMap
	 * @param memberNo
	 * @param schemeNo
	 * @param basePath
	 * @return
	 */
	public Map<String, String> mergePdfDocuments(LinkedHashMap<String, LinkedHashMap<String, String>> specificationMap,
			String memberNo, String schemeNo, String basePath) {
		Map<String, String> mergedDocuments = new HashMap<String, String>();
		String docType ="";
		String fileName = "";
		String outFile ="";
		OutputStream outputStream;
		PdfWriter pdfWriter;
		String path ="";
		String pageNumbersList = "";
		String[] pageNumberArray ;
		int currentPgNo;
		PdfImportedPage pdfImportedPage;
		PdfContentByte contentByte;
		LOG.info("Document splitting and merging specification : Started" + specificationMap.toString());
		try {
			for (Map.Entry<String, LinkedHashMap<String, String>> outerEntry : specificationMap.entrySet()) {
				docType = outerEntry.getKey();
				fileName = "";
				if (schemeNo != "" || schemeNo != null) {
					fileName = schemeNo;
				}
				if (memberNo != "" || memberNo != null) {
					fileName = fileName + "-" + memberNo;
				}
				outFile = basePath + "\\" + fileName + "-" + docType + ".pdf";
				com.itextpdf.text.Document document = new com.itextpdf.text.Document();
				outputStream = new FileOutputStream(outFile);
				pdfWriter = PdfWriter.getInstance(document, outputStream);
				document.open();
				contentByte = pdfWriter.getDirectContent();
				for (Map.Entry<String, String> innerEntry : outerEntry.getValue().entrySet()) {
					path = innerEntry.getKey();
					pageNumbersList = innerEntry.getValue();
					pageNumberArray = pageNumbersList.split(",");
					PdfReader reader = new PdfReader(path);
					for (String number : pageNumberArray) {
						document.newPage();
						currentPgNo = Integer.parseInt(number);
						pdfImportedPage = pdfWriter.getImportedPage(reader, currentPgNo);
						contentByte.addTemplate(pdfImportedPage, 0, 0);
					}
					pdfWriter.freeReader(reader);
				}
				mergedDocuments.put(docType, outFile);
				outputStream.flush();
				document.close();
				outputStream.close();
				pdfWriter.close();
				LOG.info("Document Type " + docType + " created");

			}
			LOG.info(mergedDocuments.toString());

		} catch (IOException e) {
			LOG.info("Merging Docuemnts  : Exception MergeDocuments -- " + e);
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getStackTrace());
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getCause());

		} catch (DocumentException e) {
			LOG.info("Merging Docuemnts  : Exception MergeDocuments -- " + e);
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getStackTrace());
			LOG.info("Merging Docuemnts : Exception MergeDocuments -- " + e.getCause());

		}

		return mergedDocuments;
	}

	/**
	 * Upload the documents to the Document Repository and return the DmsIDs
	 * 
	 * @param documentMap
	 * @param jsonObject
	 * @param server
	 * @return
	 */
	public JsonObject uploadDocumentToRepository(Map<String, String> documentMap, JsonObject jsonObject,
			String server) {
		JsonObject response = new JsonObject();
		JsonObject body = new JsonObject();
		JsonObject innerBody = new JsonObject();
		String memberNo = "";
		String schemeNo = "";
		String workType = "";
		Long processOid = 0L;
		String currentFile = "";
		String currentDocType = "";
		String uploadUri = server.trim() + ApplicationConstants.REPOSITORY_UPLOAD_URL.getValue().trim();
		LOG.info("The URL for uploading the document : " + uploadUri);

		try {
			URL uploadUrl = new URL(uploadUri);
			HttpURLConnection addConnection;
			Date date = new Date();
			String system = server.split("/")[2].trim().split(":")[0];
			for (Map.Entry<String, String> mapEntry : documentMap.entrySet()) {
				addConnection = (HttpURLConnection) uploadUrl.openConnection();
				addConnection.setRequestMethod("POST");
				addConnection.setRequestProperty("Content-Type", "application/json");
				addConnection.setDoOutput(true);
				currentFile = mapEntry.getValue();
				currentDocType = mapEntry.getKey();
				body.addProperty("createdBy", "PSL Scanning & Indexing");
				body.addProperty("system", system);
				body.addProperty("file", currentFile);
				body.add("keywords", null);

				innerBody.addProperty("documentType", currentDocType);

				innerBody.addProperty("uploadedDate", date.toString());
				if (jsonObject.get("memberNo") != null) {
					memberNo = jsonObject.get("memberNo").getAsString();
					innerBody.addProperty("memberNo", memberNo);
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

				
				body.add("keywords2", innerBody);

				String input = body.toString();
				OutputStream os = addConnection.getOutputStream();
				os.write(input.getBytes());
				os.flush();

				if (addConnection.getResponseCode() != 200) {
					throw new RuntimeException("Failed to Upload Document " + currentFile
							+ " to Repository with HTTP error code : " + addConnection.getResponseCode()
							+ " and reponse message : " + addConnection.getResponseMessage());
				}

				BufferedReader br = new BufferedReader(new InputStreamReader((addConnection.getInputStream())));
				String output;
				LOG.info("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					LOG.info(output);
					if (output.matches("[0-9]+")) {
						response.addProperty(currentDocType, output);
					}
				}
			}
			jsonObject = null;
		} catch (IOException e) {
			LOG.info("Uploading Docuemnts  : Exception UploadDocuments -- " + e);
			LOG.info("Uploading Docuemnts : Exception UploadDocuments -- " + e.getStackTrace());
			LOG.info("Uploading Docuemnts : Exception UploadDocuments -- " + e.getCause());

		}

		return response;
	}
}
