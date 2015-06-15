package amt2fhir;

import org.jgrapht.graph.DefaultEdge;

public class Edge extends DefaultEdge {

    private static final long serialVersionUID = 1L;

    public Long getSource() {
        return (Long) super.getSource();
    }

    public Long getTarget() {
        return (Long) super.getTarget();
    }

}
