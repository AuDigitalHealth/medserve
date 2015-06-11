package amt2fhir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;

public class Amt2Fhir {

    private static final String TERMINOLOGY_PATH = "src/main/resources/AMT_Release_AU1000168_20150531/RF2Release/Snapshot/Terminology/";
    private static final String LANG_REFSET_PATH = "src/main/resources/AMT_Release_AU1000168_20150531/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en-AU_AU1000168_20150531.txt";
    private static final String REFSET_PATH = "src/main/resources/AMT_Release_AU1000168_20150531/RF2Release/Snapshot/Refset/Content/";

    public static void main(String args[]) throws IOException {
        Concepts concepts = new Concepts();

        readFile(new File(TERMINOLOGY_PATH + "sct2_Concept_Snapshot_AU1000168_20150531.txt"),
            s -> concepts.handleConceptRow(s));

        readFile(new File(TERMINOLOGY_PATH + "sct2_Relationship_Snapshot_AU1000168_20150531.txt"),
            s -> concepts.handleRelationshipRow(s));

        readFile(new File(TERMINOLOGY_PATH + "sct2_Description_Snapshot-en-AU_AU1000168_20150531.txt"),
            s -> concepts.handleDescriptionRow(s));

        readFile(new File(LANG_REFSET_PATH), s -> concepts.handleLanguageRefsetRow(s));

        readFile(new File(REFSET_PATH + "der2_ccsRefset_StrengthSnapshot_AU1000168_20150531.txt"),
            s -> concepts.handleDatatypeRefsetRow(s));

        readFile(new File(REFSET_PATH + "der2_ccsRefset_UnitOfUseSizeSnapshot_AU1000168_20150531.txt"),
            s -> concepts.handleDatatypeRefsetRow(s));

        readFile(new File(REFSET_PATH + "der2_ccsRefset_UnitOfUseQuantitySnapshot_AU1000168_20150531.txt"),
            s -> concepts.handleDatatypeRefsetRow(s));

        readFile(new File(REFSET_PATH + "der2_cciRefset_SubpackQuantitySnapshot_AU1000168_20150531.txt"),
            s -> concepts.handleDatatypeRefsetRow(s));

        concepts.calculateTransitiveClosure();

    }

    public static void readFile(File f, Consumer<String[]> c) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            reader.lines().skip(1).forEach(s -> c.accept(s.split("\t")));
        }
    }
}
