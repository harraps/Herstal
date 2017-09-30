package triangulation

import org.joml.Vector2d

enum class Order { LEXICOGRAPHIC, BOTTOM_UP }

class HalfEdge (var origin : Vertex) {

    var twin : HalfEdge
        get() = _twin
        set(edge) {
            _twin = edge
            _twin._twin = this
        }
    var prev : HalfEdge
        get() = _prev
        set(edge) {
            _prev = edge
            _prev._next = this
        }
    var next : HalfEdge
        get() = _next
        set(edge) {
            _next = edge
            _next._prev = this
        }

    private lateinit var _twin : HalfEdge
    private lateinit var _prev : HalfEdge
    private lateinit var _next : HalfEdge

    var faceLeft : Boolean = false
    var label : VertexLabel = VertexLabel.NOT_SET


    // constructor used to link two Vertices and form an edge
    constructor(A : Vertex, B : Vertex, twinEdge : HalfEdge?,
            faceOnLeft  : Boolean, faceOnRight : Boolean) : this(A)
    {
        origin = A
        faceLeft = faceOnLeft

        // call constructor a second time for the twin half edge
        if (twinEdge == null)
            _twin = HalfEdge(B, A, this, faceOnRight, faceOnLeft)
        else
            _twin = twinEdge

        val edges = origin.edges()
        if (edges.size == 0) {
            origin.incident = this
            prev = twin

        } else if (edges.size == 1) {
            val edge = edges[0]
            prev = edge.twin
            twin.next = edge

        } else {
            // we must place our new edge between A and B
            val vector = vector()
            var before = edges.last()

            for (after in edges) {
                val a1 = vector.angle(before.vector())
                val a2 = vector.angle(after .vector())
                // our vector must be between V1 and V2 in that order
                if ((a1>0 && a2<0)
                        || (a2>0 && a1>0 && a2>a1)
                        || (a1<0 && a2<0 && a1<a2))
                {
                    twin.next = after
                    prev = before.twin
                    break
                }
                before = after
            }
        }
    }

    // destructor used to relink half-edges (call on only one of the half-edge)
    fun destructor() {
        prev.next = twin.next
        twin.prev.next = next

        if (origin.incident == this) {
            if (twin.next == this)
                origin.incident = null
            else
                origin.incident = twin.next
        }
        if (twin.origin.incident == twin) {
            if (next == twin)
                twin.origin.incident = null
            else
                twin.origin.incident = next
        }
    }

    fun vector() : Vector2d {
        return origin.vector(twin.origin)
    }

    // mark the vertex
    fun markVertex() {
        if (label == VertexLabel.NOT_SET) {
            val vec1 = vector()
            val vec2 = prev.twin.vector()
            val angle = vec1.angle(vec2)

            // characterize the vertex
            var def = 0b0000
            if      (vec1.x == 0.0 && vec2.x == 0.0) def = def or 0b0011
            else if (vec1.x >= 0.0 && vec2.x >= 0.0) def = def or 0b0001
            else if (vec1.x <= 0.0 && vec2.x <= 0.0) def = def or 0b0010
            if      (angle > 0) def = def or 0b0100
            else if (angle < 0) def = def or 0b1000

            // set the type based on the characterization
            when (def) {
                0b0101 -> label = VertexLabel.BEGIN
                0b1001 -> label = VertexLabel.SPLIT
                0b0110 -> label = VertexLabel.END
                0b1010 -> label = VertexLabel.MERGE
                else   -> label = VertexLabel.REGULAR
            }
        }
    }

    // gather all of the edges on the loop
    fun loop(edges : MutableCollection<HalfEdge>) {
        var current = this
        do {
            edges.add(current)
            current = current.next
        } while (current != this)
    }

    // select one particular edge from the loop
    fun selectFromLoop(
            comp : (e1 : HalfEdge, e2 : HalfEdge) -> Boolean) : HalfEdge
    {
        var current  = this
        var selected = this
        do {
            current = current.next
            if (comp(current, selected))
                selected = current
        } while(current != this)
        return selected
    }
}

fun shorter(e1 : HalfEdge, e2 : HalfEdge) : Boolean {
    return e1.vector().length() < e2.vector().length()
}
fun longest(e1 : HalfEdge, e2 : HalfEdge) : Boolean {
    return e1.vector().length() > e2.vector().length()
}