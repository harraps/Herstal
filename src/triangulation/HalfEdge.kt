package triangulation

import org.joml.Vector2d

enum class Order { LEXICOGRAPHIC, BOTTOM_UP }

class HalfEdge (val origin : Vertex, val fixed : Boolean, val faceLeft : Polygon?) {

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

    var label : VertexLabel = VertexLabel.NOT_SET
    var index : Int = -1 // indicate the index of the edge (for swapping)

    // constructor used to link two Vertices and form an edge
    constructor(A          : Vertex   , B           : Vertex   ,
                fixedEdge  : Boolean  , twinEdge    : HalfEdge?,
                faceOnLeft : Polygon? , faceOnRight : Polygon? )
            : this(A, fixedEdge, faceOnLeft)
    {
        // call constructor a second time for the twin half edge
        _twin = twinEdge ?: HalfEdge(B, A, fixedEdge, this, faceOnRight, faceOnLeft)

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
        return origin vector twin.origin
    }

    // mark the vertex
    fun markVertex() {
        if (label == VertexLabel.NOT_SET) {
            val vec1 = vector()
            val vec2 = prev.twin.vector()
            val angle = vec1.angle(vec2)

            // characterize the vertex
            // use vectors direction along x axis
            var def = 0b0000
            def = if (vec1.x == 0.0 && vec2.x == 0.0) def or 0b0011
            else  if (vec1.x >= 0.0 && vec2.x >= 0.0) def or 0b0001
            else  if (vec1.x <= 0.0 && vec2.x <= 0.0) def or 0b0010
            else def
            // use angle sign
            def = when {
                angle > 0 -> def or 0b0100
                angle < 0 -> def or 0b1000
                else      -> def
            }

            // set the type based on the characterization
            label = when (def) {
                0b0101 -> VertexLabel.BEGIN
                0b1001 -> VertexLabel.SPLIT
                0b0110 -> VertexLabel.END
                0b1010 -> VertexLabel.MERGE
                else   -> VertexLabel.REGULAR
            }
        }
    }

    // gather all of the edges on the loop
    infix fun gatherLoopIn(edges : MutableCollection<HalfEdge>) {
        var current = this
        do {
            edges.add(current)
            current = current.next
        } while (current != this)
    }

    // select one particular edge from the loop
    infix fun selectFromLoop(
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

    override fun toString() : String {
        val type = when (label) {
            VertexLabel.BEGIN   -> "begin"
            VertexLabel.MERGE   -> "merge"
            VertexLabel.REGULAR -> "regular"
            VertexLabel.SPLIT   -> "split"
            VertexLabel.END     -> "end"
            else -> "not set"
        }
        return origin.toString() + " -> " + twin.origin.toString() + " : " + type
    }
}

fun shorter(e1 : HalfEdge, e2 : HalfEdge) : Boolean {
    return e1.vector().length() < e2.vector().length()
}
fun longest(e1 : HalfEdge, e2 : HalfEdge) : Boolean {
    return e1.vector().length() > e2.vector().length()
}