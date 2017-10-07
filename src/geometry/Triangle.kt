package geometry

import org.joml.Vector2d

class Triangle(val a : Vector2d, val b : Vector2d, val c : Vector2d) {

    var ab : Triangle? = null
    var bc : Triangle? = null
    var ca : Triangle? = null

    fun inside(point : Vector2d) : Boolean {
        return inTriangle(
                point.x, point.y,
                a.x, a.y,
                b.x, b.y,
                c.x, c.y )
    }
}