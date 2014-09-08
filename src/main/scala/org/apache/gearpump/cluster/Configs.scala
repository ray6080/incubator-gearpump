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

package org.apache.gearpump.cluster

import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory
import org.apache.gearpump.streaming.task.TaskId
import org.apache.gearpump.util.DAG

/**
 * Immutable configuration
 */
class Configs(val config: Map[String, _])  extends Serializable{
  import org.apache.gearpump.cluster.Configs._

  def withValue(key: String, value: Any) = {
    Configs(config + (key->value))
  }

  def getInt(key : String) = {
    config.getInt(key)
  }


  def withAppId(appId : Int) = withValue(APPID, appId)
  def appId : Int = config.getInt(APPID)

  def withAppDescription(appDesc : Application) = withValue(APP_DESCRIPTION, appDesc)

  def appDescription : Application = config.getAnyRef(APP_DESCRIPTION).asInstanceOf[Application]

  def withMaster(master : ActorRef) = withValue(MASTER, master)
  def master : ActorRef = config.getAnyRef(MASTER).asInstanceOf[ActorRef]

  def withAppMaster(appMaster : ActorRef) = withValue(APP_MASTER, appMaster)
  def appMaster : ActorRef = config.getAnyRef(APP_MASTER).asInstanceOf[ActorRef]

  def withExecutorId(executorId : Int) = withValue(EXECUTOR_ID, executorId)
  def executorId = config.getInt(EXECUTOR_ID)

  def withSlots(slots : Int) = withValue(SLOTS, slots)
  def slots = config.getInt(SLOTS)

  def withAppManager(appManager : ActorRef) = withValue(APP_MANAGER, appManager)
  def appManager : ActorRef = config.getAnyRef(APP_MANAGER).asInstanceOf[ActorRef]

  def withTaskId(taskId : TaskId) =  withValue(TASK_ID, taskId)
  def taskId : TaskId = config.getAnyRef(TASK_ID).asInstanceOf[TaskId]

  def withDag(taskDag : DAG) = withValue(TASK_DAG, taskDag)
  def dag : DAG = config.getAnyRef(TASK_DAG).asInstanceOf[DAG]

}

object Configs {

  //config for construction of appMaster
  val APPID = "appId"
  val APP_DESCRIPTION =  "appDescription"
  val SINGLETON_MANAGER = "singleton"
  val MASTER_PROXY = "masterproxy"
  val MASTER = "master"
  val MASTER_WATCHER = "masterwatcher"

  val WORKER = "worker"
  val APP_MANAGER = "appManager"

  //config for construction of executor
  val APP_MASTER = "appMaster"
  val EXECUTOR_ID = "executorId"
  val SLOTS = "slots"

  val TASK_ID = "taskId"
  val TASK_DAG = "taskDag"

  def empty = new Configs(Map.empty[String, Any])

  def apply(config : Map[String, _]) = new Configs(config)

  //for production
  val SYSTEM_DEFAULT_CONFIG = ConfigFactory.load()

  val MASTER_CONFIG = {
    val config = SYSTEM_DEFAULT_CONFIG
    if (config.hasPath(MASTER)) {
      config.getConfig(MASTER).withFallback(config)
    } else {
      config
    }
  }

  val WORKER_CONFIG = {
    val config = SYSTEM_DEFAULT_CONFIG
    if (config.hasPath(WORKER)) {
      config.getConfig(WORKER).withFallback(config)
    } else {
      config
    }
  }

  private implicit class MapHelper(config: Map[String, _]) {
    def getInt(key : String) : Int = {
      config.get(key).get.asInstanceOf[Int]
    }

    def getAnyRef(key: String) : AnyRef = {
      config.get(key).get.asInstanceOf[AnyRef]
    }
  }
}
