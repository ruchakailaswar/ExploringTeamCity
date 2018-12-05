package com.psl.beans;

public enum ApplicationConstants {
	META_DATA_SCHEME_NO("SchemeNo"),META_DATA_MEMBER_NO("MemberNo"),META_DATA_REQUEST_TYPE("RequestType"),
	META_DATA_DOCUMENT_TYPES("DocumentType"),PROCESS_ATTACHMENTS("PROCESS_ATTACHMENTS"),SECURITY_DOCUMENTS_PATH("/securityDocuments"),ROOT_FOLDER("/liberty"),
	SWITCH_FORM_DOC_TYPE("Investment Portfolio Selection Form: Member level"),SWITCHES_AND_ELECTIONS("Switches And Elections"),
	DOC_ID("docId"),META_DATA_MEMBER_FIRSTNAME("MemberFirstName"),META_DATA_MEMBER_LASTNAME("MemberLastName"),META_DATA_SCHEMENAME("SchemeName"),
	META_DATA_NATIONALID("NationalIdNo"), META_DATA_DATERECEIVED("DateReceived"), INDEX_DATA_ID("IndexData"), WORK_TYPE_ID("WorkType"),
	OPERATIONS_PRODUCT("OperationsProduct"), CLAIMS_PRODUCT("ClaimsProduct"), NEW_BUSINESS_PRODUCT("NewBusinessProduct"),ROUTE_BY_WORKTYPE_PROCESS_DEFINITION("RouteByWorkType"),
	REFERENCE_ID_XPATH("ProcessOid"),REVIEW_PROCESS_EXCEPTION_ID("ReviewProcessException"),
	URL_STRING("/ipp-portal/services/rest/document-triage/documents/"),EXCEL_DUMP_NAME("Migration Report"), PROCESS_DATA_PATH_ID("ProcessData"), MEMBER_NAME_XPATH("MemberName"),
	EXCEL_DUMP_KEYS("processInstanceOID,memberNumber,schemeNumber,workType,action"),REPOSITORY_FETCH_URL("Libcor.Rest.Dms/StorageProvider/Centera/GetDocument?CenteraKeyId="),
	REPOSITORY_UPLOAD_URL("Libcor.Rest.Dms/StorageProvider/Centera/AddDocument");


	private String value;

	private ApplicationConstants(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
