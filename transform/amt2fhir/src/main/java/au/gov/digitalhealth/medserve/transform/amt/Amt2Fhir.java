package au.gov.digitalhealth.medserve.transform.amt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import au.gov.digitalhealth.medserve.transform.amt.cache.AmtCache;
import au.gov.digitalhealth.medserve.transform.processor.FhirServerMedicationResourceProcessor;
import au.gov.digitalhealth.medserve.transform.processor.FileWritingMedicationResourceProcessor;

public class Amt2Fhir {

    private static final String INPUT_FILE_OPTION = "i";

    private static final String PBS_INPUT_FILE_OPTION = "pbs";

    private static final String FHIR_OPTION = "url";

    private static final String OUTPUT_FILE_OPTION = "o";

    private static final Logger logger = Logger.getLogger(AmtCache.class.getCanonicalName());


    public static void main(String args[]) throws IOException, URISyntaxException {
        long start = System.currentTimeMillis();
        Options options = new Options();

        options.addOption(Option.builder(INPUT_FILE_OPTION)
            .longOpt("inputFile")
            .argName("AMT_ZIP_FILE_PATH")
            .hasArg()
            .desc("Input AMT release ZIP file")
            .required(true)
            .build());
        options.addOption(Option.builder(PBS_INPUT_FILE_OPTION)
            .longOpt("pbsExtract")
            .argName("PBS_ZIP_FILE_PATH")
            .hasArg()
            .desc("Input PBS extract ZIP file")
            .required(true)
            .build());
        options.addOption(Option.builder(FHIR_OPTION)
            .argName("FHIR_SERVER_BASE_URL")
            .hasArg()
            .desc("FHIR server URL to post Medication Resources to "
                    + "- e.g. http://fhir-dev.healthintersections.com.au/open/")
            .build());
        options.addOption(Option.builder(OUTPUT_FILE_OPTION)
            .longOpt("outputDirectory")
            .argName("OUTPUT_DIR")
            .hasArg()
            .desc("Output directory to write out Medication Resources as files")
            .build());

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(options, args);

            AmtMedicationResourceGenerator generator = new AmtMedicationResourceGenerator(
                FileSystems.getDefault().getPath(line.getOptionValue(INPUT_FILE_OPTION)),
                FileSystems.getDefault().getPath(line.getOptionValue(PBS_INPUT_FILE_OPTION)));

            if (!line.hasOption(OUTPUT_FILE_OPTION) && !line.hasOption(FHIR_OPTION)) {
                throw new ParseException("At least one output mode -o or -url must be specified");
            }

            if (line.hasOption(FHIR_OPTION)) {
                generator.process(new FhirServerMedicationResourceProcessor(line.getOptionValue(FHIR_OPTION), 200000));
            }

            if (line.hasOption(OUTPUT_FILE_OPTION)) {
                generator.process(new FileWritingMedicationResourceProcessor(
                    FileSystems.getDefault().getPath(line.getOptionValue(OUTPUT_FILE_OPTION))));
            }

        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Amt2Fhir", options);
        }

        logger.info("Completed processing in " + (System.currentTimeMillis() - start));
    }
}
