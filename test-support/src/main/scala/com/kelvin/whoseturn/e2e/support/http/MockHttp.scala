package com.kelvin.whoseturn.e2e.support.http

import java.net.ServerSocket

object MockHttp {

  protected def getFreePort: Int = {
    val socket = new ServerSocket(0)
    socket.setReuseAddress(true)

    val localPort = socket.getLocalPort
    socket.close()

    localPort
  }
}
