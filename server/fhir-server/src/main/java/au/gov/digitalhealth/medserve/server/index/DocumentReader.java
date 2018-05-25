package au.gov.digitalhealth.medserve.server.index;

import org.apache.lucene.document.Document;
import org.hl7.fhir.instance.model.api.IBaseResource;

import au.gov.digitalhealth.medserve.server.indexbuilder.constants.FieldNames;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

public final class DocumentReader {

    private static IParser fhirParser = FhirContext.forDstu3().newJsonParser();

    private DocumentReader() {
        throw new AssertionError("Static method helper class not to be constructed!");
    }
    
    public static <T extends IBaseResource> T getResourceFromDocument(Document doc, Class<T> clazz) {
        try {
            return fhirParser
                .parseResource(clazz,
                    doc.getField(FieldNames.JSON).stringValue());
        } catch (DataFormatException e) {
            throw new RuntimeException("Failed reading document " + doc, e);
        }
    }

}
