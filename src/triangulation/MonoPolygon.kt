package triangulation

open class MonoPolygon (val outer : HalfEdge) {

    open fun edges() : MutableCollection<HalfEdge> {
        val edges = mutableSetOf<HalfEdge>()
        outer.loop(edges)
        return edges
    }

    fun ordered(order : Order) : List<HalfEdge> {
        val list = edges()
        val comp = when (order) {
            Order.LEXICOGRAPHIC ->
                compareBy<HalfEdge>({it.origin.x},{it.origin.y})
            Order.BOTTOM_UP     ->
                compareBy<HalfEdge>({it.origin.y},{it.origin.x})
        }
        return list.sortedWith(comp)
    }

    fun triangulate(edges : MutableCollection<HalfEdge>) {
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
            if (edge.origin.areNeignbors(top.origin)) {
                sameBranch(diagonals, stack, edge, lowerBranch)
            } else {
                differentBranch(diagonals, stack, edge, lowerBranch)
                lowerBranch = !lowerBranch
            }
        }
        stack.removeAt(stack.lastIndex)
        differentBranch(diagonals, stack, ordereds.last(), lowerBranch)

        // create all of the edges
        for (diag in diagonals)
            edges.add(HalfEdge(diag.first, diag.second, null, true, true))
        edges.add(ordereds.last())
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
    for (i in (stack.lastIndex - 1) downTo 0) {
        val other = stack[i].origin
        if (canTraceDiagonal(diagonals, stack, i, edge.origin, other,
                             lowerBranch, 0.0))
        {
            if (lowerBranch)
                diagonals.add(Pair(edge.origin, other))
            else
                diagonals.add(Pair(other, edge.origin))
            stack.removeAt(i)
        } else break
    }
    stack.add(edge)
}

private fun canTraceDiagonal(
        diagonals : MutableList<Pair<Vertex, Vertex>>,
        stack : MutableList<HalfEdge>, stop : Int, a : Vertex, b : Vertex,
        lowerBranch : Boolean, threshold : Double) : Boolean
{
    // check that the segment AB is on the right side of the edge "top of stack"
    var edge = if (lowerBranch) stack.last() else stack.last().prev
    val vector = if (lowerBranch) edge.vector().normalize().negate() else edge.vector().normalize()
    val vectorAB = a.vector(b).normalize()

    val angle = vector.angle(vectorAB)
    if (if (lowerBranch) angle >= 0 else angle <= 0) return false
    for (i in (stack.lastIndex - 1) downTo  stop) {
        edge = if (lowerBranch) stack[i].prev else stack[i]
        if (intersect(edge.origin.coords(), edge.twin.origin.coords(),
                      a.coords(), b.coords(), true) ||
                aligned(edge.origin.coords(), a.coords(), b.coords(), threshold))
            return false
    }
    return true
}