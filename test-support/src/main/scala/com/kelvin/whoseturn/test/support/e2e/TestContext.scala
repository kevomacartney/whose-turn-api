package com.kelvin.whoseturn.test.support.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.{Method, Request, Response, Uri}

final case class TestContext(serverPort: Int) {
  val serverUrl              = s"127.0.0.1:$serverPort"
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def executeRequestWithResponse[T](url: String, method: Method = Method.GET)(handler: Response[IO] => T): T = {
    val fullUrl              = s"http://$serverUrl$url"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullUrl))
    executeRequestWithResponse(request)(handler)
  }

  def executeRequestWithResponse[T](request: Request[IO])(handler: Response[IO] => T): T = {
    httpClient.run(request).use(resp => IO(handler(resp))).unsafeRunSync()
  }

}
