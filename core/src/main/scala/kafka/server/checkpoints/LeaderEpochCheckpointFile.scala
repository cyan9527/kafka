/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kafka.server.checkpoints

import java.io._
import java.util.regex.Pattern

import kafka.server.LogDirFailureChannel
import kafka.server.epoch.EpochEntry

import scala.collection._

trait LeaderEpochCheckpoint {
  def write(epochs: Seq[EpochEntry]): Unit
  def read(): Seq[EpochEntry]
}

object LeaderEpochCheckpointFile {

  // 之前写代码连文件名都要可配置，其实根本没有用，固化的一般作为类的私有属性更方便，容易变化的应该由调用者传递下来
  private val LeaderEpochCheckpointFilename = "leader-epoch-checkpoint"
  private val WhiteSpacesPattern = Pattern.compile("\\s+")
  private val CurrentVersion = 0

  def newFile(dir: File): File = new File(dir, LeaderEpochCheckpointFilename)

  // 实现对应的formatter
  //epoch?
  object Formatter extends CheckpointFileFormatter[EpochEntry] {

    override def toLine(entry: EpochEntry): String = s"${entry.epoch} ${entry.startOffset}"

    override def fromLine(line: String): Option[EpochEntry] = {
      WhiteSpacesPattern.split(line) match {
        case Array(epoch, offset) =>
          Some(EpochEntry(epoch.toInt, offset.toLong))
        case _ => None
      }
    }

  }
}

/**
  * This class persists a map of (LeaderEpoch => Offsets) to a file (for a certain replica)
  */
class LeaderEpochCheckpointFile(val file: File, logDirFailureChannel: LogDirFailureChannel = null) extends LeaderEpochCheckpoint {
  import LeaderEpochCheckpointFile._

  val checkpoint = new CheckpointFile[EpochEntry](file, CurrentVersion, Formatter, logDirFailureChannel, file.getParentFile.getParent)

  def write(epochs: Seq[EpochEntry]): Unit = checkpoint.write(epochs)

  def read(): Seq[EpochEntry] = checkpoint.read()
}
