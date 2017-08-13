package online.medserve.transform.rxnorm.cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import online.medserve.transform.rxnorm.enumeration.RxNormReltaionshipType;
import online.medserve.transform.rxnorm.enumeration.RxNormType;
import online.medserve.transform.rxnorm.model.Concept;
import online.medserve.transform.util.FileUtils;

public class RxNormCache {

    private static final Logger logger = Logger.getLogger(RxNormCache.class.getCanonicalName());

    // private SimpleDirectedGraph<Long, Edge> graph = new SimpleDirectedGraph<>(Edge.class);

    private Map<Long, Concept> conceptCache = new HashMap<>();

    private Map<RxNormType, Set<Concept>> typeCache = new HashMap<>();

    public RxNormCache(FileSystem rxnormZip) throws IOException {
        RxNormFileVisitor visitor = new RxNormFileVisitor();

        Files.walkFileTree(rxnormZip.getPath("/"), visitor);

        FileUtils.readFile(visitor.getConceptFile(), s -> handleConceptRow(s), true, "[|]");
        FileUtils.readFile(visitor.getRelationshipFile(), s -> handleRelationshipRow(s), true, "[|]");
        FileUtils.readFile(visitor.getAttributeFile(), s -> handleAttributeRow(s), true, "[|]");

        calculateTransitiveClosure();

        typeCache.forEach((a, b) -> logger.info("Loaded " + b.size() + " " + a.getCode() + " - " + a.getDescription()));
    }

    private void handleConceptRow(String[] row) {
        String suppress = row[16]; // Suppressible flag. Values = N, O, Y, or E. N - not suppressible. O - Specific
                                   // individual names (atoms) set as Obsolete because the name is no longer
                                   // provided by
                                   // the original source. Y - Suppressed by RxNorm editor. E - unquantified,
                                   // non-prescribable drug with related quantified, prescribable drugs. NLM
                                   // strongly
                                   // recommends that users not alter editor-assigned suppressibility.
        if (suppress.equals("N") && RxNormType.isEnumValue(row[12])) {
            long rxcui = Long.parseLong(row[0]); // RxNorm Unique identifier for concept (concept ID)
            String lat = row[1]; // Language of Term
            String rxaui = row[7]; // Unique identifier for atom (RxNorm Atom ID)
            String saui = row[8]; // Source asserted atom identifier [optional]
            String scui = row[9]; // Source asserted concept identifier [optional]
            String sdui = row[10]; // Source asserted descriptor identifier [optional]
            String sab = row[11]; // Source abbreviation
            RxNormType tty = RxNormType.fromCode(row[12]); // Term type in source
            String code = row[13]; // "Most useful" source asserted identifier (if the source vocabulary has more than
                                   // one
                                   // identifier), or a RxNorm-generated source entry identifier (if the source
                                   // vocabulary
                                   // has none.)
            String str = row[14]; // String

            String cvf = row[17]; // Content view flag. RxNorm includes one value, '4096', to denote inclusion in the
                               // Current Prescribable Content subset. All rows with CVF='4096' can be found in the
                               // subset.

            Concept concept = conceptCache.get(rxcui);
            if (concept == null) {
                concept = new Concept(rxcui);
                conceptCache.put(rxcui, concept);
            }

            concept.addCode(sab, tty, code, str);
            if (sab.equals("RXNORM") && tty.isBaseType()) {
                if (typeCache.get(tty) == null) {
                    typeCache.put(tty, new HashSet<>());
                }
                typeCache.get(tty).add(concept);
            }
        }
    }

    private void handleRelationshipRow(String[] row) {
        String sab = row[10]; // Abbreviation of the source of relationship
        if (sab.equals("RXNORM") && !row[0].isEmpty()) {
            Long rxcui1 = Long.parseLong(row[0]); // Unique identifier of first concept
            String rxaui1 = row[1]; // Unique identifier for first atom
            String stype1 = row[2]; // The name of the column in RXNCONSO.RRF that contains the identifier used for the
                                    // first concept or first atom in source of the relationship (e.g., 'AUI' or 'CUI').
            String rel = row[3]; // Relationship of second concept or atom to first concept or atom
            Long rxcui2 = Long.parseLong(row[4]); // Unique identifier of second concept
            String rxaui2 = row[5]; // Unique identifier for second atom
            String stype2 = row[6]; // The name of the column in RXNCONSO.RRF that contains the identifier used for the
                                    // second concept or second atom in the source of the relationship (e.g., 'AUI' or
                                    // 'CUI').
            RxNormReltaionshipType rela = RxNormReltaionshipType.fromCode(row[7]); // Additional (more specific)
                                                                                   // relationship label (optional)
            String rui = row[8]; // Unique identifier for relationship
            String rg = row[13]; // Machine generated and unverified indicator (optional)
            String suppress = row[14]; // Suppressible flag. Values = Y, E, or N. Reflects the suppressible status of
                                       // the
                                       // relationship; not yet in use. See also SUPPRESS in MRCONSO.RRF and MRDEF.RRF
                                       // and
                                       // MRREL.RRF in the UMLS Reference Manual.
            String cvf = row[15]; // Content view flag. RxNorm includes one value, '4096', to denote inclusion in the
                                  // Current Prescribable Content subset. All rows with CVF='4096' can be found in the
                                  // subset.
            if (conceptCache.containsKey(rxcui1) && conceptCache.containsKey(rxcui2)) {
                conceptCache.get(rxcui2).addRelationship(rela, conceptCache.get(rxcui1));
            }
        }

    }

    private void handleAttributeRow(String[] s) {
        // TODO Auto-generated method stub
    }

    private void calculateTransitiveClosure() {

    }

    public Collection<Long> getDescendantOf(Long... id) {
        // Set<Long> result = graph.incomingEdgesOf(id[0])
        // .stream()
        // .map(e -> e.getSource())
        // .collect(Collectors.toSet());
        //
        // for (int i = 1; i < id.length; i++) {
        // result.retainAll(graph.incomingEdgesOf(id[i])
        // .stream()
        // .map(e -> e.getSource())
        // .collect(Collectors.toSet()));
        // }
        //
        // return result;
        return null;
    }

    public Concept getConcept(Long id) {
        return conceptCache.get(id);
    }

    public Set<Concept> getConceptsOfType(RxNormType type) {
        return typeCache.get(type);
    }

}
