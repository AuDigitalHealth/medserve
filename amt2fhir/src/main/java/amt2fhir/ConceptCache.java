package amt2fhir;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.SimpleDirectedGraph;

public class ConceptCache {
    private static final String PREFERRED = "900000000000548007";

    private static final String FSN = "900000000000003001";

    private static final String AMT_MODULE_ID = "900062011000036108";

    private static final Logger logger = Logger.getLogger(ConceptCache.class.getCanonicalName());

    private SimpleDirectedGraph<Long, Edge> graph = new SimpleDirectedGraph<>(Edge.class);

    private Map<Long, Concept> conceptCache = new HashMap<>();

    private Set<Long> preferredDescriptionIdCache = new HashSet<>();

    private Map<Long, Relationship> relationshipCache = new HashMap<>();

    private Map<Long, Concept> mps = new HashMap<>();
    private Map<Long, Concept> mpuus = new HashMap<>();
    private Map<Long, Concept> mpps = new HashMap<>();
    private Map<Long, Concept> tpuus = new HashMap<>();
    private Map<Long, Concept> tpps = new HashMap<>();
    private Map<Long, Concept> ctpps = new HashMap<>();

    public ConceptCache(FileSystem fileSystem) throws IOException {

        TerminologyFileVisitor visitor = new TerminologyFileVisitor();

        Files.walkFileTree(fileSystem.getPath("/"), visitor);

        readFile(visitor.getConceptFile(), s -> handleConceptRow(s));
        readFile(visitor.getRelationshipFile(), s -> handleRelationshipRow(s));
        readFile(visitor.getLanguageRefsetFile(), s -> handleLanguageRefsetRow(s));
        readFile(visitor.getDescriptionFile(), s -> handleDescriptionRow(s));

        for (Path file : visitor.getDatatypePropertyFiles()) {
            readFile(file, s -> handleDatatypeRefsetRow(s));
        }

        calculateTransitiveClosure();

        graph.incomingEdgesOf(AmtConcept.CTPP.getId())
            .stream()
            .map(e -> e.getSource())
            .forEach(id -> ctpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.TPP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !ctpps.keySet().contains(id))
            .forEach(id -> tpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MPP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !tpps.keySet().contains(id))
            .filter(id -> !ctpps.keySet().contains(id))
            .forEach(id -> mpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.TPUU.getId())
            .stream()
            .map(e -> e.getSource())
            .forEach(id -> tpuus.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MPUU.getId())
            .stream()
            .filter(id -> !tpuus.keySet().contains(id))
            .map(e -> e.getSource())
            .forEach(id -> mpuus.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !tpuus.keySet().contains(id))
            .filter(id -> !mpuus.keySet().contains(id))
            .forEach(id -> mps.put(id, conceptCache.get(id)));

        logger.info("Loaded " + ctpps.size() + " CTPPs " + tpps.size() + " TPPs " + mpps.size() + " MPPs "
            + tpuus.size() + " TPUUs " + mpuus.size() + " MPUUs " + mps.size() + " MPs");
    }

    public Map<Long, Concept> getMps() {
        return mps;
    }

    public Map<Long, Concept> getMpuus() {
        return mpuus;
    }

    public Map<Long, Concept> getMpps() {
        return mpps;
    }

    public Map<Long, Concept> getTpuus() {
        return tpuus;
    }

    public Map<Long, Concept> getTpps() {
        return tpps;
    }

    public Map<Long, Concept> getCtpps() {
        return ctpps;
    }

    private void readFile(Path path, Consumer<String[]> consumer) throws IOException {
        Files.lines(path).skip(1).forEach(s -> consumer.accept(s.split("\t")));
    }

    private void handleConceptRow(String[] row) {
        if (isActive(row) && isAmtModule(row)) {
            long conceptId = Long.parseLong(row[0]);
            graph.addVertex(conceptId);
            conceptCache.put(conceptId, new Concept(conceptId));
        }
    }

    private void handleRelationshipRow(String[] row) {
        long source = Long.parseLong(row[4]);
        long destination = Long.parseLong(row[5]);

        if (isActive(row) && isAmtModule(row) && AttributeType.isEnumValue(row[7]) && graph.containsVertex(source)
            && graph.containsVertex(destination)) {

            AttributeType type = AttributeType.fromIdString(row[7]);
            Concept sourceConcept = conceptCache.get(source);
            Concept destinationConcept = conceptCache.get(destination);

            if (type.equals(AttributeType.IS_A)) {
                graph.addEdge(source, destination);
                sourceConcept.addParent(destinationConcept);
            } else {
                long groupId = Long.parseLong(row[6]);
                Map<Long, Set<Relationship>> relationshipGroups = sourceConcept.getRelationshipGroups();
                if (!relationshipGroups.containsKey(groupId)) {
                    relationshipGroups.put(groupId, new HashSet<Relationship>());
                }
                Relationship relationship = new Relationship(sourceConcept, destinationConcept, type);
                relationshipGroups.get(groupId).add(relationship);
                relationshipCache.put(Long.parseLong(row[0]), relationship);
            }
        }
    }

    private void handleDescriptionRow(String[] row) {
        String conceptId = row[4];
        if (isActive(row) && isAmtModule(row) && conceptCache.containsKey(conceptId)) {
            String descriptionId = row[0];
            String term = row[7];
            Concept concept = conceptCache.get(conceptId);
            if (row[6].equals(FSN)) {
                concept.setFullSpecifiedName(term);
            } else if (preferredDescriptionIdCache.contains(descriptionId)) {
                concept.setPreferredTerm(term);
            }
        }
    }

    private void handleLanguageRefsetRow(String[] row) {
        if (isActive(row) && isAmtModule(row) && row[6].equals(PREFERRED)) {
            preferredDescriptionIdCache.add(Long.parseLong(row[5]));
        }
    }

    private void handleDatatypeRefsetRow(String[] row) {
        if (isActive(row) && isAmtModule(row)) {
            Relationship relationship = relationshipCache.get(Long.parseLong(row[5]));
            long unitId = Long.parseLong(row[7]);
            relationship.setDatatypeProperty(new DataTypeProperty(row[8], conceptCache.get(unitId),
                AttributeType.fromIdString(row[4])));
        }
    }

    private void calculateTransitiveClosure() {
        try (LoggingTimer l = new LoggingTimer(logger, "calculate transitive closure")) {
            TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(graph);
        }
    }

    private boolean isActive(String[] row) {
        return row[2].equals("1");
    }

    private boolean isAmtModule(String[] row) {
        return row[3].equals(AMT_MODULE_ID);
    }

}
