package uk.gov.justice.laa.ccms.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

@Service
public class ReferenceDataService {

	private Logger logger = LoggerFactory.getLogger(ReferenceDataService.class);

	public <T> List<T> loadObjectList(Class<T> type, String fileName) {
		try {
			MappingIterator<T> entities = new CsvMapper().readerWithTypedSchemaFor(type).readValues(fileName);

			return entities.readAll();
		} catch (Exception e) {
			logger.error("Error occurred while loading object list from file " + fileName, e);
			return Collections.emptyList();
		}
	}

}
