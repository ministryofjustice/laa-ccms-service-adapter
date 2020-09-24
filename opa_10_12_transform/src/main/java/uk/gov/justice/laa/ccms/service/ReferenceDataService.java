package uk.gov.justice.laa.ccms.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ReferenceDataService {

	private Logger logger = LoggerFactory.getLogger(ReferenceDataService.class);
	
	@Autowired
	private ApplicationContext context;

	public <T> List<T> loadObjectList(Class<T> type, String fileName) {
		try {
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = mapper.schemaFor(type).withSkipFirstDataRow(true).withColumnSeparator(',');

			InputStream resource = new ClassPathResource(
					fileName).getInputStream();

			MappingIterator<T> it = mapper.readerFor(type).with(schema)
					  .readValues(resource);
	
			return it.readAll();
		} catch (Exception e) {
			logger.error("Error occurred while loading object list from file " + fileName, e);
			return Collections.emptyList();
		}
	}

}
