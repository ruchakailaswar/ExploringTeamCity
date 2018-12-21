package com.psl.applications;

import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.DocumentInfo;
import org.eclipse.stardust.engine.api.runtime.DocumentManagementService;
import org.eclipse.stardust.engine.api.runtime.Folder;
import org.eclipse.stardust.engine.api.runtime.FolderInfo;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import com.itextpdf.text.DocumentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class IppServiceTest {
	
	private Map<String,Object> properties;
	private IppService ippService;
	private DocumentManagementService dms;
	private WorkflowService wfs;
	private List<Document> documents = new ArrayList<Document>();
	private Response response;
	private String input;
	
	@Before
	public void setUpMocks(){
		ippService = spy(new IppService());//could have defined in setUp mock method, but verifying methods on global object is a bad practice!!
		dms = mock(DocumentManagementService.class);
		wfs = mock(WorkflowService.class);
	}

	@After
	public void destroyMocks(){
		ippService = null;
		dms = null;
		wfs = null;
		documents.clear();
		response = null;
		input = null;
	}
	
	
	@Test @Ignore
	public void testGetProcessData() {

		//Not Needed
	
	}

	@Test @Ignore
	public void testGetActivitiesDetailsForIndexing() {
		//Not needed
	}
	
	
	@Test 
	public void testGetAttachedDocuments() {

		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document1.getName()).thenReturn("fileName1.pdf");
		when(document1.getDateCreated()).thenReturn(new Date());

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document2.getName()).thenReturn("fileName2.pdf");
		when(document2.getDateCreated()).thenReturn(new Date(new Date().getTime()+60*60*1000));
		
		documents.add(document1);
		documents.add(document2);
		Object object = (Object)documents;
		
		//when(ippService.getWorkflowService()).thenReturn(wfs); - actual call happens here
		doReturn(wfs).when(ippService).getWorkflowService();
		when(wfs.getInDataPath(anyLong(), anyString())).thenReturn(object);
		
		List<Document> respDocuments = ippService.getAttachedDocuments(1L);
		
		verify(ippService).getProcessData(anyLong(), anyString());
		
		assertNotNull(respDocuments);
		assertEquals(documents.size(), respDocuments.size());
		
		
	}

	@Test @Ignore
	public void testPreparePaginatedJSONData() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testPrerpareDocJSON() {
		fail("Not yet implemented");
	}

	@Test
	public void testSaveDocumentinIpp() {
		
		byte[] content = {1,2,3,4,5};
		
		Document document = mock(Document.class);

		
		Document document1 = mock(Document.class);


		Document document2 = mock(Document.class);

		
		documents.add(document1);
		documents.add(document2);
		Object object = (Object)documents;
		
		doReturn(wfs).when(ippService).getWorkflowService();
		when(wfs.getInDataPath(anyLong(), anyString())).thenReturn(object);
		
		properties = ippService.populateDocumentProperties("schemeNo", "memberNo", "workType", "docTypes");
		
		Folder folder = mock(Folder.class);
		doReturn(dms).when(ippService).getDocumentManagementService();
		when(dms.getFolder(anyString(),eq(0))).thenReturn(folder);
		when(dms.createDocument(anyString(), any(DocumentInfo.class), any(byte[].class), eq(null))).thenReturn(document);
		
		Document doc = ippService.saveDocumentinIpp(content, "fileName", "application/pdf", 1L, properties);
		
		verify(ippService).createDocument(any(byte[].class), anyString(), anyString(), anyLong(), any(Map.class));
		verify(dms).createDocument(anyString(), any(DocumentInfo.class), any(byte[].class), eq(null));
		
		assertNotNull(doc);
		
		
	}

	@Test @Ignore
	public void testCreateDocument() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAddDocumentsToProcessAttachments() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testSetProcessData() {
		fail("Not yet implemented");
	}

	@Test
	public void testPrepareJSONData() {
		
		Document document = mock(Document.class);
		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document.getName()).thenReturn("fileName.pdf");
		when(document.getDateCreated()).thenReturn(new Date());
		when(document.getRevisionName()).thenReturn("1.1");
		properties = ippService.populateDocumentProperties("schemeNo", "memberNo", "workType", "docTypes");
		when(document.getProperties()).thenReturn(properties);
		
		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document1.getName()).thenReturn("fileName1.pdf");
		when(document1.getDateCreated()).thenReturn(new Date());
		when(document1.getRevisionName()).thenReturn("1.1");
		when(document1.getProperties()).thenReturn(properties);

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document2.getName()).thenReturn("fileName2.pdf");
		when(document2.getDateCreated()).thenReturn(new Date(new Date().getTime()+60*60*1000));
		when(document2.getRevisionName()).thenReturn("1.1");
		when(document2.getProperties()).thenReturn(properties);
		

		documents.add(document);
		documents.add(document1);
		documents.add(document2);
		
		response = ippService.prepareJSONData(documents);
		
		verify(ippService).prerpareDocJSON(any(List.class));
		verify(ippService,times(3)).getDocumentTypeString(any(Document.class));
		//verify(ippService,times(3)).getCSVS(any(List.class)); Static methods can't be verified
		
		assertNotNull(response);
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		
		
	}

	@Test @Ignore
	public void testGetDocumentTypeString() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testGetCSVS() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDocumentContent() {
		
		byte[] content = {1,2,3,4,5};
		input = "{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919";
		
		Document document = mock(Document.class);
		when(document.getName()).thenReturn("fileName.pdf");
		
		doReturn(dms).when(ippService).getDocumentManagementService();
		when(dms.getDocument(anyString())).thenReturn(document);
		when(dms.retrieveDocumentContent(anyString())).thenReturn(content);
		
		String response = ippService.getDocumentContent(input);
		
		verify(dms).retrieveDocumentContent(anyString());
		
		assertNotNull(response);
		
		
	}

	@Test @Ignore
	public void testGetActivitiesDetails() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testGetProcessDetails() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testFetchProcessInstancesForIds() {
		fail("Not yet implemented");
	}

	@Test
	public void testParseJSONObject() {
		
		input = "{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919\","
				+ "\r\n\"memberNo\":\"U062674C\",\r\n\"schemeNo\":\"0090004424\",\r\n\"processOid\":\"2214262\","
				+ "\r\n\"docTypes\":\"Previous Rules\",\r\n\"workType\":\"Death Educator Claim\"\r\n}";
		
		JsonObject response = ippService.parseJSONObject(input);
		
		assertNotNull(response);
		String responseString = response.toString();
		
		assertEquals(input.replaceAll("(\\r|\\n)", ""), responseString);
		
	}

	@Test @Ignore
	public void testUpdateNonSecureDocument() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testPopulateDocumentProperties() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testDeleteDocuments() {
		//No Need
	}

	@Test
	public void testRemoveDocument() {
		
		input = "{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919";
		
		Document document = mock(Document.class);
		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6920");
		
		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6921");
		
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		documents.add(document1);
		documents.add(document2);
		Object object = (Object)documents;

		doReturn(wfs).when(ippService).getWorkflowService();
		when(wfs.getInDataPath(anyLong(), anyString())).thenReturn(object);
		doNothing().when(wfs).setOutDataPath(anyLong(), anyString(),any());
		doReturn(dms).when(ippService).getDocumentManagementService();
		doNothing().when(dms).removeDocument(anyString());
		
		ippService.removeDocument(input, 1L);
		
		verify(dms).removeDocument(anyString());
		verify(wfs).setOutDataPath(anyLong(), anyString(), any());
		
	
	}

	@Test @Ignore
	public void testFetchProcessInstances() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testFetchAndSetProcessDataPaths() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAbortProcessInstances() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAbortProcess() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testStartProcessById() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAttachDocuments() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testUploadDocinFolder() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testGetProcessCount() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testConvertDocumentTIFtoPDF() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testConvertTifAttachmentToPDF() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testChangeConfigVariablesandDeployModel() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testCreateUserGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testConvertMultiStripTifToPDF() {
		
		byte[] convertedContent = null;
		File file = new File("src/test/resources/sample.tif");
		byte[] tiffContent = new byte[(int) file.length()]; 
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			fis.read(tiffContent); //read file into bytes[]
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			convertedContent = ippService.convertMultiStripTifToPDF(tiffContent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try (FileOutputStream fos = new FileOutputStream("src/test/resources/sample.pdf")) {
			   try {
				fos.write(convertedContent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			   //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
		File convertedFile = new File("src/test/resources/sample.pdf");
		
		assertNotNull(convertedContent);
		assertNotEquals(tiffContent, convertedContent);
		assertEquals(true,convertedFile.exists());
		assertEquals(true,convertedFile.getName().endsWith(".pdf"));
		convertedFile.delete();
		
	}

	@Test @Ignore
	public void testChangeStatus() {
		fail("Not yet implemented");
	}

	@Test
	public void testConvertDocTiffToPDFBatch() {
		
		ippService = spy(new IppService());
		dms = mock(DocumentManagementService.class);
		
		byte[] sampleByteArray = {1,2,3,4};
		
		Document document = mock(Document.class);
		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6920");
		when(document.getName()).thenReturn("fileName.tiff");
		
		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document1.getName()).thenReturn("fileName1.TIF");

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6921");
		when(document2.getName()).thenReturn("fileName2.pdf");

		List<String> documentIdList = new ArrayList<String>();
		documentIdList.add(document.getId());
		documentIdList.add(document1.getId());
		documentIdList.add(document2.getId());
		
		documents.add(document);
		documents.add(document1);
		documents.add(document2);
		
		
		doReturn(dms).when(ippService).getDocumentManagementService();
		when(dms.getDocuments(documentIdList)).thenReturn(documents);
		when(dms.retrieveDocumentContent(anyString())).thenReturn(sampleByteArray);
		try {
			doReturn(sampleByteArray).when(ippService).convertMultiStripTifToPDF(any(byte[].class));
		} catch (IOException | DocumentException e) {
			e.printStackTrace();
		} 
		
		when(dms.updateDocument(any(Document.class),any(byte[].class), anyString(), anyBoolean(), anyString(), anyString(), anyBoolean())).thenReturn(null);
		

		
		ippService.convertDocTiffToPDFBatch(documentIdList);
		
		verify(dms).getDocuments(documentIdList);
		verify(document,times(4)).getName();
		verify(document1,times(4)).getName();
		verify(document2).getName();
		verify(dms,times(2)).retrieveDocumentContent(anyString());
		try {
			verify(ippService,times(2)).convertMultiStripTifToPDF(any(byte[].class));
		} catch (IOException | DocumentException e) {
			e.printStackTrace();
		}
		
		verify(document).setName(anyString());
		verify(document1).setName(anyString());
		
		
		
		
		
	}

	@Test @Ignore
	public void testReadExternalPropertiesFile() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testWriteExternalPropertiesFile() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testDoctypeAssignment() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testAbortActiveProcess() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testGetDocumentDetailsObject() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testParseCSV() {
		fail("Not yet implemented");
	}

	@Test @Ignore
	public void testCreateCSV() {
		fail("Not yet implemented");
	}

}
