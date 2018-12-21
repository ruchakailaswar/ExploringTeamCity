package com.psl.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

//import static org.powermock.api.mockito.PowerMockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.eclipse.stardust.engine.api.runtime.Document;
import org.eclipse.stardust.engine.api.runtime.DocumentManagementService;
import org.eclipse.stardust.engine.api.runtime.WorkflowService;
import org.eclipse.stardust.engine.extensions.dms.data.DocumentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.modules.junit4.PowerMockRunner;
import com.psl.applications.IppService;
import com.psl.services.DocumentServices;

@RunWith(MockitoJUnitRunner.class)
//@RunWith(PowerMockRunner.class)
public class DocumentServicesTest {



	private  Map<String,Object> properties;
	private IppService ippService;
	private DocumentManagementService dms;
	private WorkflowService wfs;
	private List<Document> documents = new ArrayList<Document>();
	private DocumentServices documentServices = new DocumentServices();
	private Response resp;
	private String input;

	@Before
	public void setUpMocks(){
		ippService = mock(IppService.class);
		dms = mock(DocumentManagementService.class);
		wfs = mock(WorkflowService.class);
		documentServices.setIppService(ippService);

	}

	@After
	public void destroyMocks(){
		ippService = null;
		dms = null;
		wfs = null;
		documents.clear();
		resp = null;
		input = null;
	}


	@Test
	public void testGetProcessDocuments(){


		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document1.getName()).thenReturn("fileName1.pdf");
		when(document1.getRevisionName()).thenReturn("1.1");
		when(document1.getDateCreated()).thenReturn(new Date());

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document2.getName()).thenReturn("fileName2.pdf");
		when(document2.getRevisionName()).thenReturn("1.1");
		when(document2.getDateCreated()).thenReturn(new Date(new Date().getTime()+60*60*1000));

		//stub(method(Document.class, "getId")).toReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		//PowerMockito is required!! but it is incompatible with Mockito library!!

		//doReturn("1.1").when(any(Document.class)).getRevisionName(); Mockito always requires a mock object or spy object!!

		documents.add(document1);
		documents.add(document2);
		Object object = (Object)documents;

		doCallRealMethod().when(ippService).getAttachedDocuments(anyLong());
		doCallRealMethod().when(ippService).getProcessData(anyLong(),anyString());
		when(ippService.getWorkflowService()).thenReturn(wfs);
		when(wfs.getInDataPath(anyLong(),anyString())).thenReturn(object);
		when(ippService.populateDocumentProperties(anyString(), anyString(), anyString(), anyString())).thenCallRealMethod();
		when(ippService.prepareJSONData(any(List.class))).thenCallRealMethod();
		when(ippService.prerpareDocJSON(any(List.class))).thenCallRealMethod();
		when(ippService.getDocumentTypeString(any(Document.class))).thenReturn("docTypes");
		//when(ippService.getCSVS(any(List.class))).thenCallRealMethod(); static method can't be mocked!!

		properties = ippService.populateDocumentProperties("schemeNo", "memberNo", "workType", "docTypes");
		when(document1.getProperties()).thenReturn(properties);
		when(document2.getProperties()).thenReturn(properties);

		resp = documentServices.getProcessDocuments("3456");
		verify(ippService).getAttachedDocuments(anyLong());
		verify(ippService).getProcessData(anyLong(),anyString());
		verify(wfs).getInDataPath(anyLong(),anyString());
		verify(ippService).prepareJSONData(any(List.class));
		verify(ippService).prerpareDocJSON(any(List.class));
		verify(ippService, times(documents.size())).getDocumentTypeString(any(Document.class));

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void editDocumentTypesTest(){
		input = "{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919\",\r\n\"memberNo\":\"U062674C\",\r\n\"schemeNo\":\"0090004424\",\r\n\"processOid\":\"2214262\",\r\n\"docTypes\":\"Previous Rules\",\r\n\"workType\":\"Death Educator Claim\"\r\n}";

		Document document = mock(Document.class);
		//DocumentType docType = mock(DocumentType.class);

		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document.getName()).thenReturn("fileName.pdf");
		when(document.getPath()).thenReturn("fileName.pdf");
		when(document.getContentType()).thenReturn("fileName.pdf");
		when(document.getDescription()).thenReturn("fileName.pdf");
		//when(document.getDocumentType()).thenReturn(docType);

		documents.add(document);
		Object object = (Object)documents;

		when(ippService.populateDocumentProperties(anyString(), anyString(), anyString(), anyString())).thenCallRealMethod();
		properties = ippService.populateDocumentProperties("schemeNo", "memberNo", "workType", "docTypes");

		when(ippService.parseJSONObject(anyString())).thenCallRealMethod();    
		when(ippService.getDocumentManagementService()).thenReturn(dms);
		when(ippService.getDocumentManagementService().getDocument(anyString())).thenReturn(document);
		when(document.getProperties()).thenReturn(properties);
		//when(ippService.updateNonSecureDocument(anyString(), anyLong(), any(Map.class))).thenReturn(document);
		when(ippService.updateNonSecureDocument(anyString(), anyLong(), any(Map.class))).thenCallRealMethod();
		when(ippService.getDocumentManagementService().updateDocument(any(Document.class),anyBoolean(),anyString(),anyString(),anyBoolean())).thenReturn(document);
		//when(ippService.getDocumentManagementService().moveDocument(anyString(),anyString())).thenReturn(document);
		doCallRealMethod().when(ippService).getProcessData(anyLong(),anyString());
		when(ippService.getWorkflowService()).thenReturn(wfs);
		when(wfs.getInDataPath(anyLong(),anyString())).thenReturn(object);
		doCallRealMethod().when(ippService).setProcessData(anyLong(), anyString(),any(List.class));
		doNothing().when(wfs).setOutDataPath(anyLong(),anyString(),any());

		resp = documentServices.editDocumentTypes(input);


		verify(ippService).updateNonSecureDocument(anyString(), anyLong(), any(Map.class));

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void getDocumentContentTest(){
		input = "{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919\"\r\n}";
		byte[] byteMockContent= {1,2,3,4,5};

		Document document = mock(Document.class);
		when(document.getName()).thenReturn("fileName.pdf");
		when(document.getRepositoryId()).thenReturn("repository");
		when(document.getSize()).thenReturn(1L);
		when(document.getContentType()).thenReturn("content-type");


		when(ippService.parseJSONObject(anyString())).thenCallRealMethod(); 
		when(ippService.getDocumentContent(anyString())).thenCallRealMethod();
		when(ippService.getDocumentManagementService()).thenReturn(dms);
		when(ippService.getDocumentManagementService().getDocument(anyString())).thenReturn(document);
		when(ippService.getDocumentManagementService().retrieveDocumentContent(anyString())).thenReturn(byteMockContent);

		resp = documentServices.getDocumentContent(input);

		verify(ippService).getDocumentContent(anyString());
		verify(ippService).parseJSONObject(anyString());
		verify(dms).retrieveDocumentContent(anyString());

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void removeDocumentTest(){
		input = "{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919\"\r\n, \"processOid\":214}";

		Document document1 = mock(Document.class);
		when(document1.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6920");

		Document document2 = mock(Document.class);
		when(document2.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");

		documents.add(document1);
		documents.add(document2);
		Object object = (Object)documents;

		when(ippService.parseJSONObject(anyString())).thenCallRealMethod(); 
		doCallRealMethod().when(ippService).removeDocument(anyString(),anyLong()); //This is how you call methods which returns void
		when(ippService.getDocumentManagementService()).thenReturn(dms);
		doNothing().when(dms).removeDocument(anyString());
		doCallRealMethod().when(ippService).getProcessData(anyLong(),anyString());
		when(ippService.getWorkflowService()).thenReturn(wfs);
		when(wfs.getInDataPath(anyLong(),anyString())).thenReturn(object);
		doCallRealMethod().when(ippService).setProcessData(anyLong(), anyString(),any(List.class));
		doNothing().when(wfs).setOutDataPath(anyLong(),anyString(),any());


		resp = documentServices.removeDocument(input);

		verify(ippService).removeDocument(anyString(),anyLong());
		verify(ippService).parseJSONObject(anyString());
		verify(ippService).getProcessData(anyLong(), anyString());
		verify(dms).removeDocument((anyString()));
		verify(ippService).setProcessData(anyLong(),anyString(), any(List.class));

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void attachDocumentsToProcessTest(){
		input = "{\"fromProcessOid\":657,\"toProcessOid\":669}";

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

		when(ippService.parseJSONObject(anyString())).thenCallRealMethod(); 
		doCallRealMethod().when(ippService).attachDocuments(anyLong(),anyLong());
		doCallRealMethod().when(ippService).getAttachedDocuments(anyLong());
		doCallRealMethod().when(ippService).addDocumentsToProcessAttachments(any(List.class),anyLong());
		doCallRealMethod().when(ippService).getProcessData(anyLong(),anyString());
		when(ippService.getWorkflowService()).thenReturn(wfs);
		when(wfs.getInDataPath(anyLong(),anyString())).thenReturn(object);
		doCallRealMethod().when(ippService).setProcessData(anyLong(), anyString(),any(List.class));
		doNothing().when(wfs).setOutDataPath(anyLong(),anyString(),any());
		//doNothing().when(ippService).setProcessData(anyLong(),anyString(), any(List.class));


		resp = documentServices.attachDocumentsToProcess(input);

		verify(ippService).attachDocuments(anyLong(),anyLong());
		verify(ippService).parseJSONObject(anyString());
		verify(ippService,times(2)).getProcessData(anyLong(), anyString());
		verify(ippService).setProcessData(anyLong(),anyString(), any(List.class));
		verify(ippService).addDocumentsToProcessAttachments(any(List.class),anyLong());

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void editMultiDocumentsTypeTest(){
		input = "{\r\n\t\"documentList\":[\r\n\t\t"
				+ "{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}76114af0-610c-4cb8-966d-e7febb11c53d\","
				+ "\r\n\"memberNo\":\"U062674C\",\r\n\"schemeNo\":\"0090004424\",\r\n\"processOid\":\"1104\","
				+ "\r\n\"docTypes\":\"Previous Rules\","
				+ "\r\n\"workType\":\"Death Educator Claim\"\r\n},"
				+ "\r\n{\r\n\"docId\":\"{urn:repositoryId:System}{jcrUuid}7c5a28db-2f08-4004-9703-d867400f5dbf\","
				+ "\r\n\"memberNo\":\"U062674C\",\r\n\"schemeNo\":\"0090004424\",\r\n\"processOid\":\"1104\","
				+ "\r\n\"docTypes\":\"Previous Rules\",\r\n\"workType\":\"Death Educator Claim\"\r\n}\r\n]\r\n}";

		Document document = mock(Document.class);
		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(document.getName()).thenReturn("fileName1.pdf");

		when(ippService.parseJSONObject(anyString())).thenCallRealMethod();
		when(ippService.getDocumentManagementService()).thenReturn(dms);
		when(ippService.getDocumentManagementService().getDocument(anyString())).thenReturn(document);
		when(ippService.populateDocumentProperties(anyString(), anyString(), anyString(), anyString())).thenCallRealMethod();
		properties = ippService.populateDocumentProperties("schemeNo", "memberNo", "workType", "docTypes");
		when(document.getProperties()).thenReturn(properties);
		when(ippService.updateNonSecureDocument(anyString(), anyLong(), any(Map.class))).thenReturn(document);


		resp = documentServices.editMultiDocumentsType(input);

		verify(ippService,times(1)).parseJSONObject(anyString());
		verify(dms,times(2)).getDocument(anyString());
		verify(ippService,times(3)).populateDocumentProperties(anyString(), anyString(), anyString(), anyString());
		verify(ippService,times(2)).updateNonSecureDocument(anyString(), anyLong(), any(Map.class));

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}



	@Test
	public void convertDocBatchTest(){
		String path = "";
		String content = "Sample String";
		InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

		Attachment attachment = mock(Attachment.class);
		DataHandler dh = mock(DataHandler.class);
		Properties properties = mock(Properties.class);



		when(attachment.getDataHandler()).thenReturn(dh);
		try {
			when(dh.getInputStream()).thenReturn(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		when(ippService.readExternalPropertiesFile(anyString())).thenReturn(properties);
		when(properties.getProperty("counter")).thenReturn("0");
		when(properties.getProperty("batch")).thenReturn("100");
		when(properties.getProperty("checkpoint")).thenReturn("10");
		doNothing().when(ippService).convertDocTiffToPDFBatch(any(List.class));
		when(properties.setProperty(eq("counter"),anyString())).thenReturn(null);
		doNothing().when(ippService).writeExternalPropertiesFile(any(Properties.class),anyString());


		resp = documentServices.convertDocBatch(attachment,path);

		verify(ippService,times(1)).readExternalPropertiesFile(anyString());
		verify(ippService,times(1)).convertDocTiffToPDFBatch(any(List.class));

		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


	@Test
	public void addFilesTest(){
		String schemeNo = "schemeNo",memberNo = "memberNo",docTypes = "docTypes",workType="workType",processOid = "1";
		String content = "Sample String";
		InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
		headers.add("Content-Disposition", "attachment;filename=sampleFile.pdf");
		headers.add("Content-Type", "application/pdf");

		Attachment attachment = mock(Attachment.class);
		DataHandler dh = mock(DataHandler.class);
		Document document = mock(Document.class);

		when(document.getId()).thenReturn("{urn:repositoryId:System}{jcrUuid}aba62189-b543-4cf6-a5aa-75353f1d6919");
		when(attachment.getDataHandler()).thenReturn(dh);
		try {
			when(dh.getInputStream()).thenReturn(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		when(attachment.getHeaders()).thenReturn(headers);
		when(ippService.populateDocumentProperties(anyString(), anyString(), anyString(), anyString())).thenCallRealMethod();
		when(ippService.saveDocumentinIpp(any(byte[].class), anyString(), anyString(), anyLong(), any(Map.class))).thenReturn(document);
		when(ippService.getDocumentTypeString(any(Document.class))).thenReturn("docTypes");


		resp = documentServices.addFiles(attachment, null,processOid,docTypes,memberNo,schemeNo,workType);

		verify(ippService).populateDocumentProperties(anyString(), anyString(), anyString(), anyString());
		verify(ippService).saveDocumentinIpp(any(byte[].class), anyString(), anyString(), anyLong(), any(Map.class));
		verify(ippService).getDocumentTypeString(any(Document.class));


		assertNotNull(resp);
		assertEquals(200, resp.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

	}


}

