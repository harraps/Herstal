package triangulation

import org.joml.Vector2d

enum class VertexLabel { NOT_SET, BEGIN, MERGE, REGULAR, SPLIT, END }

// position of the vertex
class Vertex (val x : Double, val y : Double) {

    // one incident edge
    var incident : HalfEdge? = null

    fun destructor() {
        for (edge in edges())
            edge.destructor()
    }

    fun coords() : Vector2d {
        return Vector2d(x, y)
    }

    infix fun vector(v : Vertex) : Vector2d {
        return Vector2d(v.x - x, v.y - y)
    }

    fun edges() : MutableList<HalfEdge> {
        val edges = mutableListOf<HalfEdge>()
        if (incident != null) {
            var current = incident!!
            do {
                edges.add(current)
                current = current.twin.next
            } while (current != incident)
        }
        return edges
    }

    infix fun findEdge(other : Vertex) : HalfEdge? {
        if (incident != null) {
            var current = incident!!
            do {
                if (current.twin.origin == other)
                    return current
                current = current.twin.next
            } while (current != incident)
        }
        return null
    }
    infix fun isNeighborOf(other : Vertex) : Boolean {
        return findEdge(other) != null
    }
}

// return true if the point is above the edge formed by origin and dest
fun isAbove(origin : Vertex, dest : Vertex, point : Vertex) : Boolean {
    if (point == origin) return false
    val angDst = polarAngle(origin, dest)
    val angPnt = polarAngle(origin, point)
    return if (angDst > 0)
        angPnt > angDst || angPnt < angDst - Math.PI
    else
        angDst < angPnt && angPnt < angDst + Math.PI
}

fun polarAngle(center : Vertex, point : Vertex) : Double {
    return Math.atan2(point.y - center.y, point.x - center.x)
}

fun lexicographic(a : Vertex, b : Vertex) : Boolean {
    return if (a.x == b.x) a.y < b.y else a.x < b.x
}
fun bottomUp(a : Vertex, b : Vertex) : Boolean {
    return if (a.y == b.y) a.x < b.x else a.y < b.y
}


private fun cross(u : Vector2d, v : Vector2d) : Double {
    return u.x * v.y - u.y * v.x
}

fun aligned(p : Vector2d,
            a : Vector2d, b : Vector2d, threshold : Double) : Boolean
{
    val vecC = p.sub(a)
    val vecL = b.sub(a)
    if (Math.abs(cross(vecC, vecL)) > threshold) return false
    return if (Math.abs(vecL.x) >= Math.abs(vecL.y)) {
        if (vecL.x > 0) p.x in a.x .. b.x else p.x in b.x .. a.x
    } else {
        if (vecL.y > 0) p.y in a.y .. b.y else p.y in b.y .. a.y
    }
}

fun intersect(a : Vector2d, b : Vector2d, c : Vector2d, d : Vector2d,
                 ignoreCommon : Boolean = false) : Boolean
{
    return intersection(a, b, c, d, ignoreCommon) != null
}

fun intersection(a : Vector2d, b : Vector2d, c : Vector2d, d : Vector2d,
        ignoreCommon : Boolean = false) : Vector2d?
{
    if (ignoreCommon) // ignore vertices in common
        if (a == c || b == d || a == d || b == c)
            return null
    else if (a == c || a == d) return a
    else if (b == d || b == c) return b

    val p = c.sub(a)
    val q = c.sub(b)
    val r = b.sub(a)
    val s = d.sub(c)

    val numeR = cross(p, r)
    val numeS = cross(p, s)
    val denom = cross(r, s)

    if (denom == 0.0 && numeR == 0.0) {
        // intersection if both edges are colinear
        if ((p.x < 0.0) != (q.x < 0.0) || (p.y < 0.0) != (q.y < 0.0)) {
            val result = Vector2d() // (A + B + C + D) / 4
            result.add(a, result)
            result.add(b, result)
            result.add(c, result)
            result.add(d, result)
            result.mul(0.25, result)
            return result
        }
    } else if (denom == 0.0) return null

    val t = numeS / denom
    val u = numeR / denom
    // intersection
    if (t in 0.0..1.0 && u in 0.0..1.0) {
        val result = Vector2d() // R*t + A
        result.add(r, result)
        result.mul(t, result)
        result.add(a, result)
        return result // same as (S*u + C)
    } else return null
}