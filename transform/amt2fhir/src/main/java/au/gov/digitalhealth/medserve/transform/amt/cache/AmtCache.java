package au.gov.digitalhealth.medserve.transform.amt.cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.SimpleDirectedGraph;

import au.gov.digitalhealth.medserve.transform.amt.enumeration.AmtConcept;
import au.gov.digitalhealth.medserve.transform.amt.enumeration.AttributeType;
import au.gov.digitalhealth.medserve.transform.amt.model.Concept;
import au.gov.digitalhealth.medserve.transform.amt.model.DataTypeProperty;
import au.gov.digitalhealth.medserve.transform.amt.model.Manufacturer;
import au.gov.digitalhealth.medserve.transform.amt.model.Relationship;
import au.gov.digitalhealth.medserve.transform.amt.model.Subsidy;
import au.gov.digitalhealth.medserve.transform.util.FileUtils;
import au.gov.digitalhealth.medserve.transform.util.LoggingTimer;

public class AmtCache {

    private static final SimpleDateFormat effectiveTimeFormat = new SimpleDateFormat("yyyyMMdd");

    private static final String PREFERRED = "900000000000548007";

    private static final String FSN = "900000000000003001";

    private static final String AMT_MODULE_ID = "900062011000036108";

    private static final Logger logger = Logger.getLogger(AmtCache.class.getCanonicalName());

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
    private Map<Long, Concept> substances = new HashMap<>();
    private Map<Long, Set<String>> artgIdCache = new HashMap<>();
    private Map<String, Manufacturer> manufacturers = new HashMap<>();
    private Map<String, Collection<Subsidy>> subsidies = new HashMap<>();
    private Map<String, Set<Concept>> pbscache = new HashMap<>();
    private Map<String, String> atcCodes = new HashMap<>();
    private Map<String, String> caution = new HashMap<>();
    private Map<String, String> notes = new HashMap<>();
    private Map<Long, Date> relationshipDateCache = new HashMap<>();

    public AmtCache(FileSystem amtZip, FileSystem pbsExtract) throws IOException {
        processAmtFiles(amtZip);
        if (pbsExtract != null) {
            processPbsFiles(pbsExtract);
        }
    }

    private void processPbsFiles(FileSystem pbsExtract) throws IOException {
        PbsFileVisitor visitor = new PbsFileVisitor();

        Files.walkFileTree(pbsExtract.getPath("/"), visitor);

        FileUtils.readFile(visitor.getManufacturerFile(), s -> handleManufacturerFile(s), false, "!");
        FileUtils.readFile(visitor.getNoteFile(), s -> handleNoteFile(s), false, "\t");
        FileUtils.readFile(visitor.getCautionFile(), s -> handleCautionFile(s), false, "\t");
        FileUtils.readFile(visitor.getAtcFile(), s -> handleAtcFile(s), false, "!");
        FileUtils.readFile(visitor.getAmtFile(), s -> handlePbsAmtFile(s), false, "!");
        FileUtils.readFile(visitor.getDrugFile(), s -> handlePbsDrugFile(s), false, "!");
        FileUtils.readFile(visitor.getIndicationFile(), s -> handleLinkFile(s), false, "\t");
    }

    private void handleLinkFile(String[] s) {
        String pbsCode = s[0];
        Collection<Subsidy> list = subsidies.get(pbsCode);
        if (list != null) {
            String noteId = s[5];
            String cautionId = s[6];

            for (Subsidy subsidy : list) {
                subsidy.addNote(notes.get(noteId));
                subsidy.addCaution(caution.get(cautionId));
            }
        }

    }

    private void handleCautionFile(String[] s) {
        this.caution.put(s[0], s[1]);
    }

    private void handleNoteFile(String[] s) {
        this.notes.put(s[0], s[1]);
    }

    private void handleAtcFile(String[] s) {
        atcCodes.put(s[0], s[1]);
    }

    private void handlePbsDrugFile(String[] s) {
        String pbsCode = s[4];
        Collection<Subsidy> list = subsidies.get(pbsCode);
        if (list != null) {
            for (Subsidy subsidy : list) {
                subsidy.setRestriction(s[5]);
                subsidy.addAtcCode(Pair.of(s[1], atcCodes.get(s[1])));
            }
        }
    }

    private void handleManufacturerFile(String[] s) {
        manufacturers.put(s[0],
            new Manufacturer(s[0], s[1], s[2], s.length == 4 ? s[3] : null, s.length == 5 ? s[4] : null));
    }

    private void handlePbsAmtFile(String[] row) {
        String programCode = row[0];
        String pbsCode = row[1];
        String manufacturerCode = row[2];
        long tppId = Long.parseLong(row[9]);
        Long mppId = null;
        if (!row[6].isEmpty()) {
            mppId = Long.parseLong(row[6]);
        }
        String commExManPrice = row[13];
        String manExManPrice = row[14];
        Concept tpp = conceptCache.get(tppId);
        if (tpp == null) {
            logger.warning("No such TPP " + tppId + " for PBS code " + pbsCode);
        } else {
            Subsidy subsidy = new Subsidy(pbsCode, programCode, commExManPrice, manExManPrice);
            addSubsidyToCache(subsidy);
            tpp.addSubsidy(subsidy);
            addToPbsCache(pbsCode, tpp);

            tpp.setManufacturer(manufacturers.get(manufacturerCode));
            graph.incomingEdgesOf(tpp.getId())
                .stream()
                .map(e -> e.getSource())
                .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)) && id != tpp.getId())
                .forEach(id -> {
                    Concept concept = conceptCache.get(id);
                    concept.setManufacturer(manufacturers.get(manufacturerCode));
                    concept.addSubsidy(subsidy);
                    addToPbsCache(pbsCode, concept);
                });
            if (mppId != null) {
                long mpp = mppId.longValue();
                addToPbsCache(pbsCode, conceptCache.get(mppId));

                graph.incomingEdgesOf(mppId)
                    .stream()
                    .map(e -> e.getSource())
                    .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)) && id != mpp)
                    .forEach(id -> {
                        Concept concept = conceptCache.get(id);
                        addToPbsCache(pbsCode, concept);
                    });
            }
        }

    }

    private void addSubsidyToCache(Subsidy subsidy) {
        Collection<Subsidy> subsidyList = subsidies.get(subsidy.getPbsCode());
        if (subsidyList == null) {
            subsidyList = new ArrayList<>();
            subsidies.put(subsidy.getPbsCode(), subsidyList);
        }
        subsidyList.add(subsidy);
    }

    private void addToPbsCache(String pbsCode, Concept concept) {
        Set<Concept> concepts = pbscache.get(pbsCode);
        if (concepts == null) {
            concepts = new HashSet<>();
            pbscache.put(pbsCode, concepts);
        }
        concepts.add(concept);
    }

    private void processAmtFiles(FileSystem amtZip) throws IOException {
        TerminologyFileVisitor visitor = new TerminologyFileVisitor();

        Files.walkFileTree(amtZip.getPath("/"), visitor);

        FileUtils.readFile(visitor.getConceptFile(), s -> handleConceptRow(s), true, "\t");
        FileUtils.readFile(visitor.getRelationshipFile(), s -> buildLastRelationshipDateCache(s), true, "\t");
        FileUtils.readFile(visitor.getRelationshipFile(), s -> handleRelationshipRow(s), true, "\t");
        FileUtils.readFile(visitor.getLanguageRefsetFile(), s -> handleLanguageRefsetRow(s), true, "\t");
        FileUtils.readFile(visitor.getDescriptionFile(), s -> handleDescriptionRow(s), true, "\t");
        FileUtils.readFile(visitor.getArtgIdRefsetFile(), s -> handleArtgIdRefsetRow(s), true, "\t");
        FileUtils.readFile(visitor.getAssociationRefsetFile(), s -> handleAssociationRefsetRow(s), true, "\t");

        for (Path file : visitor.getDatatypePropertyFiles()) {
            FileUtils.readFile(file, s -> handleDatatypeRefsetRow(s), true, "\t");
        }

        calculateTransitiveClosure();

        graph.incomingEdgesOf(AmtConcept.CTPP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> ctpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.TPP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !ctpps.keySet().contains(id))
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> tpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MPP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !tpps.keySet().contains(id))
            .filter(id -> !ctpps.keySet().contains(id))
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> mpps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.TPUU.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> tpuus.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MPUU.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !tpuus.keySet().contains(id))
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> mpuus.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.MP.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !tpuus.keySet().contains(id))
            .filter(id -> !mpuus.keySet().contains(id))
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> mps.put(id, conceptCache.get(id)));

        graph.incomingEdgesOf(AmtConcept.SUBSTANCE.getId())
            .stream()
            .map(e -> e.getSource())
            .filter(id -> !AmtConcept.isEnumValue(Long.toString(id)))
            .forEach(id -> substances.put(id, conceptCache.get(id)));

        conceptCache.values().stream().forEach(c -> {
            c.addAncestors(
                graph.outgoingEdgesOf(c.getId())
                    .stream()
                    .map(e -> e.getTarget())
                    .collect(Collectors.<Long, Long, Concept> toMap(id -> id, id -> conceptCache.get(id))));
        });

        logger.info("Loaded " + ctpps.size() + " CTPPs " + tpps.size() + " TPPs " + mpps.size() + " MPPs "
                + tpuus.size() + " TPUUs " + mpuus.size() + " MPUUs " + mps.size() + " MPs " + substances.size()
                + " Substances");
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

    private void handleConceptRow(String[] row) {
        if (isAmtModule(row)) {
            long conceptId = Long.parseLong(row[0]);
            graph.addVertex(conceptId);
            conceptCache.put(conceptId, new Concept(conceptId, isActive(row), parseDate(row[1])));
        }
    }

    private synchronized Date parseDate(String string) {
        try {
            return effectiveTimeFormat.parse(string);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse effective time " + string, e);
        }
    }

    private void buildLastRelationshipDateCache(String[] row) {
        long source = Long.parseLong(row[4]);
        long destination = Long.parseLong(row[5]);
        Date effectiveTime = parseDate(row[1]);
        Concept sourceConcept = conceptCache.get(source);

        if (isAmtModule(row) && AttributeType.isEnumValue(row[7]) && graph.containsVertex(source)
                && graph.containsVertex(destination) && !sourceConcept.isActive()) {

            if (!relationshipDateCache.containsKey(source) || relationshipDateCache.get(source).before(effectiveTime)) {
                relationshipDateCache.put(source, effectiveTime);
            }
        }
    }

    private void handleRelationshipRow(String[] row) {
        long source = Long.parseLong(row[4]);
        long destination = Long.parseLong(row[5]);
        Concept sourceConcept = conceptCache.get(source);
        Date effectiveTime = parseDate(row[1]);

        if (isAmtModule(row) && AttributeType.isEnumValue(row[7]) && graph.containsVertex(source)
                && graph.containsVertex(destination)
                && (isActive(row) || (!sourceConcept.isActive()
                        && effectiveTime.equals(relationshipDateCache.get(source))))) {
            
            AttributeType type = AttributeType.fromIdString(row[7]);
            Concept destinationConcept = conceptCache.get(destination);

            if (type.equals(AttributeType.IS_A)) {
                graph.addEdge(source, destination);
                sourceConcept.addParent(destinationConcept);
            } else {
                long groupId = Integer.parseInt(row[6]);
                if (groupId == 0) {
                    groupId = type.getId() + destinationConcept.getId() << 32;
                }
                Map<Long, Set<Relationship>> relationshipGroups = sourceConcept.getRelationshipGroups();
                if (!relationshipGroups.containsKey(groupId)) {
                    relationshipGroups.put(groupId, new HashSet<Relationship>());
                }
                Relationship relationship =
                        new Relationship(sourceConcept, destinationConcept, type, isActive(row), effectiveTime);
                relationshipGroups.get(groupId).add(relationship);
                relationshipCache.put(Long.parseLong(row[0]), relationship);
            }
            sourceConcept.updateLastModified(effectiveTime);
        }
    }

    private void handleDescriptionRow(String[] row) {
        Long conceptId = Long.parseLong(row[4]);
        Date effectiveTime = parseDate(row[1]);

        if (isActive(row) && isAmtModule(row) && conceptCache.containsKey(conceptId)) {
            String descriptionId = row[0];
            String term = row[7];
            Concept concept = conceptCache.get(conceptId);
            if (row[6].equals(FSN)) {
                concept.setFullSpecifiedName(term);
            } else if (preferredDescriptionIdCache.contains(Long.parseLong(descriptionId))) {
                concept.setPreferredTerm(term);
            }

            concept.updateLastModified(effectiveTime);
        }
    }

    private void handleLanguageRefsetRow(String[] row) {
        if (isActive(row) && isAmtModule(row) && row[6].equals(PREFERRED)) {
            preferredDescriptionIdCache.add(Long.parseLong(row[5]));
        }
    }

    private void handleDatatypeRefsetRow(String[] row) {
        if (isAmtModule(row) && relationshipCache.containsKey(Long.parseLong(row[5]))) {
            Relationship relationship = relationshipCache.get(Long.parseLong(row[5]));
            Date effectiveTime = parseDate(row[1]);

            if ((relationship.isActive() && isActive(row))
                    || (!relationship.isActive() && !isActive(row)
                            && relationshipDateCache.get(relationship.getSource().getId()).equals(effectiveTime)
                            && relationship.getEffectiveTime().equals(effectiveTime))) {
                long unitId = Long.parseLong(row[6]);
                relationship.setDatatypeProperty(new DataTypeProperty(row[8], conceptCache.get(unitId),
                    AttributeType.fromIdString(row[4])));
                relationship.getSource().updateLastModified(effectiveTime);
            }
        }
    }

    private void handleArtgIdRefsetRow(String[] row) {
        long conceptId = Long.parseLong(row[5]);
        Concept concept = conceptCache.get(conceptId);
        if ((isActive(row) || !concept.isActive()) && isAmtModule(row)) {

            Set<String> artgIds = artgIdCache.get(conceptId);
            if (artgIds == null) {
                artgIds = new HashSet<>();
            }
            artgIds.add(row[6]);
            artgIdCache.put(conceptId, artgIds);
            concept.updateLastModified(parseDate(row[1]));
        }
    }

    private void handleAssociationRefsetRow(String[] row) {
        Concept source = conceptCache.get(Long.parseLong(row[5]));
        Concept target = conceptCache.get(Long.parseLong(row[6]));
        if (isActive(row) && isAmtModule(row) && source != null && target != null) {
            long type = Long.parseLong(row[4]);
            Date effectiveTime = parseDate(row[1]);

            source.addReplacementConcept(type, target, effectiveTime);
            target.addReplacedConcept(type, source, effectiveTime);

            source.updateLastModified(effectiveTime);
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

    public Collection<Long> getDescendantOf(Long... id) {
        Set<Long> result = graph.incomingEdgesOf(id[0])
            .stream()
            .map(e -> e.getSource())
            .collect(Collectors.toSet());

        for (int i = 1; i < id.length; i++) {
            result.retainAll(graph.incomingEdgesOf(id[i])
                .stream()
                .map(e -> e.getSource())
                .collect(Collectors.toSet()));
        }

        return result;
    }

    public Concept getConcept(Long id) {
        return conceptCache.get(id);
    }

    public Set<String> getArtgId(Long id) {
        return artgIdCache.get(id);
    }

    public Map<Long, Concept> getSubstances() {
        return substances;
    }

}
