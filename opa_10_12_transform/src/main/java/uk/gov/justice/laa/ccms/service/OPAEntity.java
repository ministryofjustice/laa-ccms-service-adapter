package uk.gov.justice.laa.ccms.service;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "LOOKUP_CODE", "ENTITY_CODE", "ENTITY_LEVEL","PARENT_ENTITY_CODE","RELATIONSHIP_PUBLIC_NAME","REVERSE_REL_PUBLIC_NAME"})
public class OPAEntity {
	
	private String lookupcode;
	private String  entityCode;
	private Integer entityLevel;
	private String parentEntityCode;
	private String relationshipPublicName;
	private String reverseRelPublicName;
	
	public OPAEntity() {}
	
	public OPAEntity(String lookupcode, String entityCode, Integer entityLevel, String parentEntityCode,
			String relationshipPublicName, String reverseRelPublicName) {
		super();
		this.lookupcode = lookupcode;
		this.entityCode = entityCode;
		this.entityLevel = entityLevel;
		this.parentEntityCode = parentEntityCode;
		this.relationshipPublicName = relationshipPublicName;
		this.reverseRelPublicName = reverseRelPublicName;
	}
	public String getLookupcode() {
		return lookupcode;
	}
	public void setLookupcode(String lookupcode) {
		this.lookupcode = lookupcode;
	}
	public String getEntityCode() {
		return entityCode;
	}
	public void setEntityCode(String entityCode) {
		this.entityCode = entityCode;
	}
	public Integer getEntityLevel() {
		return entityLevel;
	}
	public void setEntityLevel(Integer entityLevel) {
		this.entityLevel = entityLevel;
	}
	public String getParentEntityCode() {
		return parentEntityCode;
	}
	public void setParentEntityCode(String parentEntityCode) {
		this.parentEntityCode = parentEntityCode;
	}
	public String getRelationshipPublicName() {
		return relationshipPublicName;
	}
	public void setRelationshipPublicName(String relationshipPublicName) {
		this.relationshipPublicName = relationshipPublicName;
	}
	public String getReverseRelPublicName() {
		return reverseRelPublicName;
	}
	public void setReverseRelPublicName(String reverseRelPublicName) {
		this.reverseRelPublicName = reverseRelPublicName;
	}
}
