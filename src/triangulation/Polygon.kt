package triangulation

class Polygon (outer : HalfEdge, val inners : Array<HalfEdge>) : MonoPolygon(outer)
{

    override fun edges() : MutableCollection<HalfEdge> {
        val edges = super.edges()
        for (inner in inners) inner.loop(edges)
        return edges
    }

    fun markVertices() {
        var current = outer
        do {
            current.markVertex()
            current = current.next
        } while (current != outer)
    }

    fun triangulate() : MutableCollection<HalfEdge> {
        val monos = cutPolygon()
        val edges = mutableSetOf<HalfEdge>()
        for (mono in monos)
            mono.triangulate(edges)
        return edges
    }

    fun cutPolygon() : MutableCollection<MonoPolygon> {
        val selecteds = mutableSetOf<HalfEdge>()
        val status    = mutableMapOf<HalfEdge, Vertex>()
        markVertices()

        val selectFun = { e1 : HalfEdge, e2 : HalfEdge ->
            lexicographic(e1.origin, e2.origin) }

        // we use the edge to stay on the outline of the polygon
        for (edge in ordered(Order.LEXICOGRAPHIC)) {
            when (edge.label) {
                VertexLabel.BEGIN -> {
                    if (innerIsAbove(edge))
                        status[edge] = edge.origin
                }
                VertexLabel.END -> {
                    status.remove(edge.prev)
                }
                VertexLabel.REGULAR -> {
                    if (innerIsAbove(edge)) {
                        val helper = if (edge.prev.label == VertexLabel.REGULAR)
                            status[edge.prev]!! else edge.origin
                        status.remove(edge.prev)
                        status[edge] = helper
                    } else {
                        val under = findUnder(status, edge.origin)
                        status[under] = edge.origin
                    }
                }
                VertexLabel.MERGE -> {
                    status.remove(edge.prev)
                    val under = findUnder(status, edge.origin)
                    status[under] = edge.origin
                }
                VertexLabel.SPLIT -> {
                    val under  = findUnder(status, edge.origin)
                    val helper = status[under]!!
                    if (!edge.origin.areNeignbors(helper))
                        selecteds.add(
                                HalfEdge(edge.origin, helper, null, true, true)
                                     )
                    status[under] = edge.origin
                    status[edge ] = edge.origin
                }
            }
        }
        // set to keep only one edge per monotone polygon
        val uniques = mutableSetOf<HalfEdge>()
        for (edge in selecteds) {
            if (edge.faceLeft)
                uniques.add(edge.selectFromLoop(selectFun))
            if (edge.twin.faceLeft)
                uniques.add(edge.twin.selectFromLoop(selectFun))
        }
        val monos = mutableSetOf<MonoPolygon>()
        for (edge in uniques)
            monos.add(MonoPolygon(edge))
        return monos
    }
}

private fun findUnder(status : MutableMap<HalfEdge, Vertex>,
                      vertex : Vertex) : HalfEdge
{
    var selected = status.iterator().next().key
    for (pair in status) {
        val vector = pair.key.vector()
        // if the edge is under the vertex
        if (pair.key.origin.y < vertex.y && vector.x > 0) {
            // if the edge is on top the selected one
            if (selected.origin.y < pair.key.origin.y)
                selected = pair.key
        }
    }
    return selected
}

private fun innerIsAbove(edge : HalfEdge) : Boolean {
    return lexicographic(edge.origin, edge.next.origin)
}