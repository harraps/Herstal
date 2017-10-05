package triangulation

class Polygon (val outer : HalfEdge, val inners : Array<HalfEdge>) {

    fun edges() : MutableCollection<HalfEdge> {
        val edges = mutableSetOf<HalfEdge>()
        outer gatherLoopIn edges
        for (inner in inners) inner gatherLoopIn edges
        return edges
    }

    infix fun ordered(order : Order) : List<HalfEdge> {
        val list = edges()
        val comp : Comparator<HalfEdge> = when (order) {
            Order.LEXICOGRAPHIC ->
                compareBy({it.origin.x},{it.origin.y})
            Order.BOTTOM_UP     ->
                compareBy({it.origin.y}, {it.origin.x})
        }
        return list.sortedWith(comp)
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
                    if (!(edge.origin isNeighborOf helper))
                        selecteds.add(HalfEdge(
                                edge.origin, helper, false, null, this, this))
                    status[under] = edge.origin
                    status[edge ] = edge.origin
                }
                else -> println("label not set")
            }
        }
        // set to keep only one edge per monotone polygon
        val uniques = mutableSetOf<HalfEdge>()
        for (edge in selecteds) {
            if (this == edge.faceLeft)
                uniques.add(edge selectFromLoop selectFun)
            if (this == edge.twin.faceLeft)
                uniques.add(edge.twin selectFromLoop selectFun)
        }
        return uniques.map { MonoPolygon(this, it) }.toMutableSet()
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