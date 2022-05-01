import sbt._

object Resolvers {
  val whoseTurnResolvers = Seq(
    Resolver.defaultLocal,
    "Confluent" at "https://packages.confluent.io/maven/",
  )
}
