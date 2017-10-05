package geometry

private fun sign(
        ax : Double, ay : Double,
        bx : Double, by : Double,
        cx : Double, cy : Double ) : Double
{
    return (ax - cx)*(by - cy) - (bx - cx)*(ay - cy)
}

// we use doubles because LUA use doubles too
fun inTriangle(
        px : Double, py : Double,
        ax : Double, ay : Double,
        bx : Double, by : Double,
        cx : Double, cy : Double ) : Boolean
{
    val a = sign(px, py, ax, ay, bx, by) < 0.0
    val b = sign(px, py, bx, by, cx, cy) < 0.0
    val c = sign(px, py, cx, cy, ax, ay) < 0.0
    return a == b == c
}