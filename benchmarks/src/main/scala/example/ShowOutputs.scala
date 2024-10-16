package example

import example.model1.Out
import example.showmacros.*

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(
  value = 1,
  jvmArgs = Array(
    "-server",
    "-Xms2g",
    "-Xmx2g",
    "-XX:NewSize=1g",
    "-XX:MaxNewSize=1g",
    "-XX:InitialCodeCacheSize=512m",
    "-XX:ReservedCodeCacheSize=512m",
    "-XX:+UseParallelGC",
    "-XX:-UseAdaptiveSizePolicy",
    "-XX:MaxInlineLevel=18",
    "-XX:+AlwaysPreTouch",
    "-XX:+UseNUMA",
    "-XX:-UseAdaptiveNUMAChunkSizing"
  )
)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ShowOutputs {

  val out: Out = Out.example

  def printObject(out: Out): String = out.showPretty()

  @Benchmark
  def showSanely: Any = printObject(out)
}
