package com.kelvin.whoseturn.support.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s._

final case class TestContext(serverPort: Int) {
  val serverUrl              = s"127.0.0.1:$serverPort"
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def executeRequestWithResponse[T](url: String, method: Method = Method.GET, body: EntityBody[IO] = EmptyBody)(
      handler: Response[IO] => T
  ): T = {
    val fullUrl              = s"http://$serverUrl$url"
    val request: Request[IO] = Request(method = method, uri = Uri.unsafeFromString(fullUrl), body = body)
    executeRequestWithResponse(request)(handler)
  }

  def executeRequestWithResponse[T](request: Request[IO])(handler: Response[IO] => T): T = {
    httpClient.run(request).use(resp => IO(handler(resp))).unsafeRunSync()
  }
}
