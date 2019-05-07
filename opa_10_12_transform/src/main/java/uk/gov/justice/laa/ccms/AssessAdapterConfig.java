package uk.gov.justice.laa.ccms;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.justice.laa.ccms.service.OPAEntity;
import uk.gov.justice.laa.ccms.service.ReferenceDataService;

@Configuration
public class AssessAdapterConfig {
	
	private ReferenceDataService referenceDataService;
	
	@Value("${ccms.ref-data-file}")
	private String opaEntityFielName;
	
	@Autowired
	public AssessAdapterConfig(ReferenceDataService referenceDataService) {
		this.referenceDataService = referenceDataService;
	}
	
	@Bean
	public Map<String,OPAEntity> loadOpaEntities(){
		return referenceDataService.loadObjectList(OPAEntity.class, opaEntityFielName)
				.stream().collect(Collectors.toMap(OPAEntity::getEntityCode, e -> e));
	}
}
