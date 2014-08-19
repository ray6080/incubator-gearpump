/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.transport

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.agent.Agent
import akka.util.Timeout
import org.apache.gearpump.transport.netty.{Context, TaskMessage}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await

case class HostPort(host: String, port: Int)

case class ExpressAddress(hostPort: HostPort, id: Int)

trait ActorLookupById {
  def lookupActor(id: Int): Option[ActorRef]
}

class Express(val system: ExtendedActorSystem) extends Extension with ActorLookupById {

  import org.apache.gearpump.transport.Express._

  import system.dispatcher
  val localActorMap = Agent(Map.empty[Int, ActorRef])
  val expressMap = Agent(Map.empty[HostPort, ActorRef])

  val localActorCount = new AtomicInteger(0)

  val conf = Map.empty[String, Any]


  var context: Context = null
  var serverPort = -1
  var localHost: HostPort = null

  lazy val init = {
    context = new Context(system, conf)
    serverPort = context.bind("netty-server", this)
    localHost = HostPort(system.provider.getDefaultAddress.host.get, serverPort)
    LOG.info(s"bining to netty server $localHost")

    system.registerOnTermination(new Runnable {
      override def run = context.term
    })
    Unit
  }

  def registerActor(actor: ActorRef): ExpressAddress = {
    init

    val id = localActorCount.incrementAndGet()

    //we use a dedicated thread here to avoid potential actor dead lock
    localActorMap.sendOff(_ + (id -> actor))

    Await.result(localActorMap.future(), Timeout(5, TimeUnit.SECONDS).duration)
    ExpressAddress(localHost, id)
  }

  def lookupActor(id: Int) = localActorMap.get().get(id)

  def lookupLocalActor(remote: ExpressAddress): Option[ActorRef] = {
    if (remote.hostPort == localHost) {
      lookupActor(remote.id)
    } else {
      None
    }
  }

  //transport to remote address
  def transport(taskMessage: TaskMessage, remote: ExpressAddress): Unit = {
    val expressActor = expressMap.get.get(remote.hostPort)
    if (expressActor.isDefined) {
      expressActor.get.tell(taskMessage, Actor.noSender)
    } else {
      expressMap.send { map =>
        val expressActor = map.get(remote.hostPort)
        if (expressActor.isDefined) {
          expressActor.get.tell(taskMessage, Actor.noSender)
          map
        } else {
          val actor = context.connect(remote.hostPort)
          actor.tell(taskMessage, Actor.noSender)
          map + (remote.hostPort -> actor)
        }
      }
    }
  }
}

object Express extends ExtensionId[Express] with ExtensionIdProvider {
  val LOG: Logger = LoggerFactory.getLogger(classOf[Express])

  override def get(system: ActorSystem): Express = super.get(system)

  override def lookup = Express

  override def createExtension(system: ExtendedActorSystem): Express = new Express(system)
}