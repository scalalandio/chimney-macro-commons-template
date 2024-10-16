package example.showmacros

class FastShowPrettyTest extends munit.FunSuite {

  test("Handle some simple use cases") {
    case class Foo(i: Int, D: Double, strs: List[String])
    case class Bar(vec: Vector[Foo])

    assertEquals(
      // I'm kinda cheating because on Scala.js Floats/Doubles have messed up toString
      // (see: https://ochrons.github.io/sjs2/doc/semantics.html#tostring-of-float-double-and-unit)
      // and I didn't want to handle it, since it's just a demo
      Bar(Vector(Foo(10, 2.3, List("a", "b", "c")))).showPretty(),
      """Bar(
        |  vec = Vector(
        |    Foo(
        |      i = 10,
        |      D = 2.3,
        |      strs = List(
        |        "a",
        |        "b",
        |        "c"
        |      )
        |    )
        |  )
        |)""".stripMargin
    )
  }
}
