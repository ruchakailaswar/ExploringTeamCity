package com.psl.beans;

public enum ApplicationConstants {
	META_DATA_SCHEME_NO("SchemeNo"),META_DATA_MEMBER_NO("MemberNo"),META_DATA_REQUEST_TYPE("RequestType"),
	META_DATA_DOCUMENT_TYPES("DocumentType"),PROCESS_ATTACHMENTS("PROCESS_ATTACHMENTS"),SECURITY_DOCUMENTS_PATH("/securityDocuments"),ROOT_FOLDER("/liberty");

	private String value;
	private ApplicationConstants (String value){
		this.value = value;
	}
	
	public String getValue(){
		return value;
	}
	
}
