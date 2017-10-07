package triangulation

import geometry.inTriangle
import org.joml.Vector2d
import java.lang.Math.abs

class TriMesh () {

    // use a list rather than a set to optimise edge swap
    val edges = mutableListOf<HalfEdge>()

    // add edges to the list
    infix fun add(edge : HalfEdge) {
        edge.index = edges.size
        edges.add(edge)
    }

    // replace edge by the new one
    fun replace(index : Int, edge : HalfEdge) {
        edge.index = index
        edges.add(index, edge)
    }

    fun legalize() {
        while(true) {
            // we need the index of the edges
            for (edge in edges)
                if (legalize(edge)) break
            break
        }
    }

    private fun legalize(edge: HalfEdge) : Boolean {
        if (isLegal(edge)) return false
        val edge1 = edge.twin.next
        val edge2 = edge.twin.prev
        swap(edge)
        legalize(edge1)
        legalize(edge2)
        return true
    }

    private fun swap(edge: HalfEdge) {
        val c = edge.     prev.origin
        val d = edge.twin.prev.origin
        val newEdge = HalfEdge(c,d,false,null,edge.faceLeft,edge.faceLeft)
        replace(edge.index, newEdge)
    }

}

private fun isLegal(edge : HalfEdge) : Boolean {
    // if the edge is fixed, it is either on the outline of the map or between two rooms
    // in both case it is necessarily legal
    if (edge.fixed) return true

    /* vertices layout
    *   B <- D
    *   | \  ^
    *   v  \ |
    *   C -> A
    * */
    val ax = edge.origin.x
    val ay = edge.origin.y
    val bx = edge.twin.origin.x
    val by = edge.twin.origin.y
    val cx = edge.prev.origin.x
    val cy = edge.prev.origin.y
    val dx = edge.twin.prev.origin.x
    val dy = edge.twin.prev.origin.y

    if (!isConvexQuad(ax,ay,bx,by,cx,cy,dx,dy)) return true

    val abc = minimalAngle(ax, ay, bx, by, cx, cy)
    val abd = minimalAngle(ax, ay, bx, by, dx, dy)
    val bcd = minimalAngle(bx, by, cx, cy, dx, dy)
    val acd = minimalAngle(ax, ay, cx, cy, dx, dy)

    val current = if (abc < abd) abc else abd
    val swapped = if (bcd < acd) bcd else acd

    return current >= swapped
}

private fun isConvexQuad(
        ax : Double, ay : Double,
        bx : Double, by : Double,
        cx : Double, cy : Double,
        dx : Double, dy : Double) : Boolean
{
    return !(
            inTriangle(ax,ay,bx,by,cx,cy,dx,dy) ||
                    inTriangle(bx,by,cx,cy,dx,dy,ax,ay) ||
                    inTriangle(cx,cy,dx,dy,ax,ay,bx,by) ||
                    inTriangle(dx,dy,ax,ay,bx,by,cx,cy) )
}

private fun minimalAngle(
        ax : Double, ay : Double,
        bx : Double, by : Double,
        cx : Double, cy : Double ) : Double
{
    val ab = Vector2d(bx - ax, by - ay)
    val bc = Vector2d(cx - bx, cy - by)
    val ca = Vector2d(ax - cx, ay - cy)

    val abc = abs(ab.negate().angle(bc))
    val bca = abs(bc.negate().angle(ca))
    val cab = abs(ca.negate().angle(ab))

    return if (abc < bca && abc < cab) abc
    else if(bca < cab) bca
    else cab
}