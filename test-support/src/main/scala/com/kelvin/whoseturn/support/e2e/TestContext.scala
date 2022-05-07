package com.kelvin.whoseturn.support.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.models.CreateTodoItemModel
import org.http4s.Method.GET
import org.http4s.client._
import org.http4s.{EntityEncoder, _}
import org.http4s.headers.Accept

final case class TestContext(serverPort: Int) {
  val serverUrl              = s"127.0.0.1:$serverPort"
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def executeRequestWithResponse[T, E](url: String, method: Method = Method.GET, body: E)(
      handler: Response[IO] => T
  )(implicit encoder: EntityEncoder[IO, E]): T = {
    val fullUrl = s"http://$serverUrl$url"
    val request: Request[IO] = Request(method = method, uri = Uri.unsafeFromString(fullUrl))
      .withEntity(body)

    executeRequestWithResponse(request)(handler)
  }

  def executeRequestWithResponse[T](request: Request[IO])(handler: Response[IO] => T): T = {
    httpClient.run(request).use(resp => IO(handler(resp))).unsafeRunSync()
  }
}
