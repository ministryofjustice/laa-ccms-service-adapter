package uk.gov.justice.laa.ccms.mapper;

import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessResponse;

public interface EntityLevelRelocationService {

   String getGlobalEntityId(com.oracle.determinations.server._10_0.rulebase.types.AssessRequest request);

   void moveGlobalAttributesAndRelationsToBaseLevel(AssessRequest request);
   
   void moveSubEntitiesToLowerLevel(AssessRequest request);

   void moveGlobalEntityToLowerLevel(AssessResponse response, String globalEntityId);
   
   void moveSubEntitiesToUpperLevel(AssessResponse response);
   
   void mapOpa10Relationships(com.oracle.determinations.server._10_0.rulebase.types.AssessResponse response);
}
