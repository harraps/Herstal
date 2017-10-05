package triangulation

import geometry.inTriangle
import org.joml.Vector2d
import java.lang.Math.abs

class Triangle() {

    lateinit var ab : HalfEdge
    lateinit var bc : HalfEdge
    lateinit var ca : HalfEdge

    constructor(edge : HalfEdge) : this() {
        ab = edge
        bc = edge.next
        ca = edge.prev

        // if the loop is not closed, we have a problem
        if (bc.next != ca)
            error("Invalid triangle !")

        ab.tri = this
        bc.tri = this
        ca.tri = this
    }

    fun destructor() {
        ab.tri = null
        bc.tri = null
        ca.tri = null
    }

    fun inside(point : Vector2d) : Boolean {
        return inTriangle(
                point.x, point.y,
                ab.origin.x, ab.origin.y,
                bc.origin.x, bc.origin.y,
                ca.origin.x, ca.origin.y )
    }
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

fun isLegal(edge : HalfEdge) : Boolean {
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