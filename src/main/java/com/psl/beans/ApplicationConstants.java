package com.psl.beans;

public enum ApplicationConstants {
	META_DATA_SCHEME_NO("SchemeNo"),META_DATA_MEMBER_NO("MemberNo"),META_DATA_REQUEST_TYPE("RequestType"),
	META_DATA_DOCUMENT_TYPES("DocumentType"),PROCESS_ATTACHMENTS("PROCESS_ATTACHMENTS"),SECURITY_DOCUMENTS_PATH("/securityDocuments"),ROOT_FOLDER("/liberty"),
	SWITCH_FORM_DOC_TYPE("Investment Portfolio Selection Form: Member level"),SWITCHES_AND_ELECTIONS("Switches And Elections"),  
	DOC_ID("docId"),
	URL_STRING("/ipp-portal/services/rest/document-triage/documents/"),EXCEL_DUMP_NAME("Migration Report"),
	EXCEL_DUMP_KEYS("processInstanceOID,memberNumber,schemeNumber,workType,action");

	private String value;

	private ApplicationConstants(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
