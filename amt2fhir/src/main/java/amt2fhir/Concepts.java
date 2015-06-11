package amt2fhir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

public class Concepts {
    private static final String PREFERRED = "900000000000548007";

    private static final String FSN = "900000000000003001";

    private static final String AMT_MODULE_ID = "900062011000036108";

    Logger logger = Logger.getLogger(Concepts.class.getCanonicalName());

    SimpleDirectedGraph<Long, DefaultEdge> graph = new SimpleDirectedGraph<Long, DefaultEdge>(DefaultEdge.class);

    Map<Long, Concept> conceptCache = new HashMap<>();

    Set<Long> preferredDescriptionIdCache = new HashSet<>();

    Map<Long, Relationship> relationshipCache = new HashMap<>();

    public void handleConceptRow(String[] row) {
        if (isActive(row) && isAmtModule(row)) {
            long conceptId = Long.parseLong(row[0]);
            graph.addVertex(conceptId);
            conceptCache.put(conceptId, new Concept(conceptId));
        }
    }

    public void handleRelationshipRow(String[] row) {
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

    public void handleDescriptionRow(String[] row) {
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

    public void handleLanguageRefsetRow(String[] row) {
        if (isActive(row) && isAmtModule(row) && row[6].equals(PREFERRED)) {
            preferredDescriptionIdCache.add(Long.parseLong(row[5]));
        }
    }

    public void handleDatatypeRefsetRow(String[] row) {
        if (isActive(row) && isAmtModule(row)) {
            Relationship relationship = relationshipCache.get(Long.parseLong(row[5]));
            long unitId = Long.parseLong(row[7]);
            relationship.setDatatypeProperty(new DataTypeProperty(row[8], conceptCache.get(unitId),
                AttributeType.fromIdString(row[4])));
        }
    }

    public void calculateTransitiveClosure() {
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
