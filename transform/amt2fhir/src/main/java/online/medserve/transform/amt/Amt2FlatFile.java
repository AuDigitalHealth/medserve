package online.medserve.transform.amt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import online.medserve.transform.amt.cache.AmtCache;
import online.medserve.transform.amt.enumeration.AmtConcept;
import online.medserve.transform.amt.enumeration.AttributeType;
import online.medserve.transform.amt.model.Concept;

public class Amt2FlatFile {

    private static final String INPUT_FILE_OPTION = "i";

    private static final String OUTPUT_FILE_OPTION = "o";

    private static final Logger logger = Logger.getLogger(Amt2FlatFile.class.getCanonicalName());

    private AmtCache conceptCache;

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
        options.addOption(Option.builder(OUTPUT_FILE_OPTION)
            .longOpt("outputFile")
            .argName("OUTPUT_FILE")
            .hasArg()
            .desc("Output directory to write out the flat file")
            .build());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            Amt2FlatFile amt2Fhir =
                    new Amt2FlatFile(FileSystems.getDefault().getPath(line.getOptionValue(INPUT_FILE_OPTION)));

            if (line.hasOption(OUTPUT_FILE_OPTION)) {
                amt2Fhir.writeFlatFile(FileSystems.getDefault().getPath(line.getOptionValue(OUTPUT_FILE_OPTION)));
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Amt2Fhir", options);
        }
        logger.info("Done in " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    private void writeFlatFile(Path path) throws IOException {
        if (!Files.exists(path.getParent())) {
            Files.createDirectory(path.getParent());
        }
        try (
                BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

            writeCsvHeader(writer);

            for (Concept ctpp : conceptCache.getCtpps().values()) {
                Collection<Concept> tpps = getParents(AmtConcept.TPP, AmtConcept.CTPP, Collections.singleton(ctpp));

                for (Concept tpp : tpps) {
                    Collection<Concept> mpps = getParents(AmtConcept.MPP, AmtConcept.TPP, Collections.singleton(tpp));
                    for (Concept mpp : mpps) {
                        Collection<Concept> mpuus = mpp.getMultipleDestinations(AttributeType.HAS_MPUU);

                        Set<Concept> mps = new HashSet<>();
                        for (Concept mpuu : mpuus) {
                            mps.addAll(getParents(AmtConcept.MP, AmtConcept.MPUU, Collections.singletonList(mpuu)));
                        }

                        mps.remove(new Concept(21220011000036103L, true, new Date()));

                        // if (mps.size() > 1) {
                        // addToCsv(writer, mps.size() > 1, mpps.size() > 1, ctpp, tpp, mpp, null);
                        // } else {
                            for (Concept mp : mps) {
                                addToCsv(writer, mps.size() > 1, mpps.size() > 1, ctpp, tpp, mpp, mp);
                            }
                        // }
                    }
                }
            }
        }
    }

    private void writeCsvHeader(BufferedWriter writer) throws IOException {
        writer.write(
            String.join(",", "Multiple MPs", "Multiple MPPs", "CTPP ID", "CTPP FSN", "TPP ID", "TPP FSN", "MPP ID",
                "MPP FSN", "MP ID", "MP FSN"));
        writer.newLine();
    }

    private void addToCsv(BufferedWriter writer, boolean multipleMps, boolean multipleMpps, Concept ctpp, Concept tpp,
            Concept mpp,
            Concept mp)
            throws IOException {
        writer.write(
            String.join(",",
                multipleMps ? "1" : "0",
                multipleMpps ? "1" : "0",
                ctpp.getId() + "", "\"" + ctpp.getFullSpecifiedName() + "\"",
                tpp.getId() + "", "\"" + tpp.getFullSpecifiedName() + "\"",
                mpp.getId() + "", "\"" + mpp.getFullSpecifiedName() + "\"",
                (mp == null ? "" : mp.getId()) + "", "\"" + (mp == null ? "" : mp.getFullSpecifiedName()) + "\""));
        writer.newLine();
    }

    private Set<Concept> getParents(AmtConcept parentType, AmtConcept current, Collection<Concept> concepts) {
        Set<Concept> parents = new HashSet<>();
        Set<Concept> allParents = new HashSet<>();
        for (Concept concept : concepts) {
            for (Concept parent : concept.getParents().values()) {
                if (!AmtConcept.isEnumValue(parent.getId() + "")) {
                    allParents.add(parent);
                    if (parent.hasAtLeastOneMatchingAncestor(parentType)
                            && !parent.hasAtLeastOneMatchingAncestor(current)) {
                        parents.add(parent);
                    }
                }
            }
        }

        if (parents.isEmpty()) {
            parents.addAll(getParents(parentType, current, allParents));
        }

        return parents;
    }

    public Amt2FlatFile(Path amtReleaseZipPath) throws IOException {
        conceptCache = new AmtCache(FileSystems.newFileSystem(
            URI.create("jar:file:" + amtReleaseZipPath.toAbsolutePath().toString()),
            new HashMap<>()), null);
    }

}
