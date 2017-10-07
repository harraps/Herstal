import triangulation.HalfEdge
import triangulation.Polygon
import triangulation.Vertex

fun main(args: Array<String>) {
    println("Herstal starting...")
    MainLua().run()
    //MainWindow().run()

    val list_ = arrayOf(
        Vertex( 0.0, 2.0),
        Vertex( 0.5, 1.0),
        Vertex( 0.0, 0.0),
        Vertex( 1.5, 0.5),
        Vertex( 3.0, 0.0),
        Vertex( 2.5, 1.0),
        Vertex( 3.0, 2.0),
        Vertex( 1.5, 1.5) )

    val list__ = arrayOf(
            Vertex( 1.25, 1.0 ),
            Vertex( 1.5 , 1.25),
            Vertex( 1.75, 1.0 ),
            Vertex( 1.5 , 0.75) )

    var outer : HalfEdge? = null
    var pv = list_.last()
    for (v in list_) {
        outer = HalfEdge(pv,v,true,null,null,null)
        pv = v
    }

    var inner : HalfEdge? = null
    pv = list__.last()
    for (v in list__) {
        inner = HalfEdge(pv,v,true,null,null,null)
        pv = v
    }

    val poly = Polygon(outer!!, arrayOf<HalfEdge>(inner!!))

    val list1 = mutableListOf<HalfEdge>()
    /*
    val monos = poly.cutPolygon()
    for (mono in monos) {
        mono.outer gatherLoopIn list1
    } // */
    //*
    val tris = poly.makeTriMesh()
    //tris.legalize()
    for (edge in tris.edges) {
        edge gatherLoopIn list1
    } // */

    val s = Success(list1)
    s.isVisible = true
}