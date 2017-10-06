
import triangulation.HalfEdge
import javax.swing.*
import java.awt.*
import java.awt.geom.*

internal class Success(val list : MutableList<HalfEdge>) : JFrame() {
    init {
        val panel = JPanel()
        contentPane.add(panel)
        setSize(450, 450)
    }

    override fun paint(g : Graphics?) {
        super.paint(g)  // fixes the immediate problem.
        val g2 = g as Graphics2D?
        for (e in list) {
            val line = makeLine(e)
            g2!!.draw(line)
        }
    }
}

private fun makeLine(edge : HalfEdge) : Line2D.Double {
    return Line2D.Double(
            edge.origin.x *       100.0 + 100.0,
            edge.origin.y *       100.0 + 100.0,
            edge.twin.origin.x *  100.0 + 100.0,
            edge.twin.origin.y *  100.0 + 100.0)
}