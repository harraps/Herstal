package triangulation

class MonoPolygon (val poly : Polygon, val outer : HalfEdge) {

    fun edges() : MutableCollection<HalfEdge> {
        val edges = mutableSetOf<HalfEdge>()
        outer gatherLoopIn edges
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

    infix fun triangulate(trimesh : TriMesh) {
        // use edges to stay on the outline of the polygon
        val ordereds  = ordered(Order.LEXICOGRAPHIC)
        val stack     = mutableListOf<HalfEdge>()
        val diagonals = mutableListOf<Pair<Vertex, Vertex>>()

        // stack : bottom->0 ; top->last
        stack.add(ordereds[0])
        stack.add(ordereds[1])

        var lowerBranch = ordereds[0].next == ordereds[1]
        for (i in 2 .. (ordereds.lastIndex - 1)) {
            val edge = ordereds[i]
            val top  = stack.last()
            // vertex is on the same branch as the top of the stack
            if (edge.origin isNeighborOf top.origin)
                sameBranch     (diagonals, stack, edge, lowerBranch)
            else {
                differentBranch(diagonals, stack, edge, lowerBranch)
                lowerBranch = !lowerBranch
            }
        }
        stack.removeAt(stack.lastIndex)
        differentBranch(diagonals, stack, ordereds.last(), lowerBranch)

        // create all of the edges
        for (diag in diagonals)
            trimesh add HalfEdge(diag.first, diag.second, false, null, poly, poly)
        trimesh add ordereds.last()
    }
}

private fun differentBranch(
        diagonals : MutableList<Pair<Vertex, Vertex>>,
        stack : MutableList<HalfEdge>, edge : HalfEdge, lowerBranch : Boolean)
{
    for (i in stack.lastIndex downTo 1) {
        if (lowerBranch)
            diagonals.add(Pair(stack[i].origin, edge.origin))
        else
            diagonals.add(Pair(edge.origin, stack[i].origin))
    }
    val top = stack.last()
    stack.clear()
    stack.add(top)
    stack.add(edge)
}

private fun sameBranch(
        diagonals : MutableList<Pair<Vertex, Vertex>>,
        stack : MutableList<HalfEdge>, edge : HalfEdge, lowerBranch : Boolean)
{
    // top of stack is end of vector
    var other = stack.last()
    for (i in stack.lastIndex downTo 1) {
        other = stack[i]
        if (canTraceDiagonal(stack, i, edge.origin, other.origin, lowerBranch))
        {
            if (lowerBranch)
                diagonals.add(Pair(edge.origin, other.origin))
            else
                diagonals.add(Pair(other.origin, edge.origin))
            stack.removeAt(i)
        } else break

    }
    stack.add(other)
    stack.add(edge)
}

private fun canTraceDiagonal(stack : MutableList<HalfEdge>, stop : Int,
                             a : Vertex, b : Vertex, lowerBranch : Boolean) : Boolean
{
    // check that the segment AB is on the right side of the edge "top of stack"
    var edge = if (lowerBranch) stack.last() else stack.last().prev

    val vector = if (lowerBranch) edge.vector().negate() else edge.vector()
    val vectorAB = a.vector(b)
    val angle = vector.angle(vectorAB)
    if (if (lowerBranch) angle >= 0 else angle <= 0) return false

    for (i in (stack.lastIndex - 1) downTo stop) {
        edge = if (lowerBranch) stack[i].prev else stack[i]
        val e1 = edge.     origin.coords()
        val e2 = edge.twin.origin.coords()
        val ac = a.coords()
        val bc = b.coords()
        if (intersect(e1,e2,ac,bc,true) || aligned(e1,ac,bc,0.00001))
            return false
    }
    return true
}