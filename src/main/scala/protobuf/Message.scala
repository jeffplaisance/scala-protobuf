// Copyright 2010 Jeff Plaisance
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.

package protobuf

import com.google.protobuf.ByteString
import java.io.{InputStream, OutputStream}

/**
 * @author jplaisance
 */

object ByteArrayByteStringImplicits {
    implicit def byteArrayToByteString(bytes:Array[Byte]):ByteString = ByteString.copyFrom(bytes)
    implicit def byteStringToByteArray(byteString:ByteString):Array[Byte] = byteString.toByteArray
}

trait Message {
    def get(i:Int):Any
    def writeTo(outputStream:OutputStream):Unit
    def writeDelimitedTo(outputStream:OutputStream):Unit
    def javaMessage:com.google.protobuf.Message
}

trait TypedMessage[A <: com.google.protobuf.Message] extends Message {
    override def javaMessage:A
}

trait MessageBuilder {
    def set(i:Int, fieldValue:Option[Any]):Unit
    def build:Message
}

trait TypedMessageBuilder[A <: TypedMessage[B], B <: com.google.protobuf.Message] extends MessageBuilder {
    override def build:A
}

trait MessageParser {
    def parseFrom(inputStream:InputStream):Message
    def parseDelimitedFrom(inputStream:InputStream):Message
    def javaToScala(message:com.google.protobuf.Message):Message
}

trait TypedMessageParser[A <: TypedMessage[B], B <: com.google.protobuf.Message] extends MessageParser {
    override def parseFrom(inputStream:InputStream):A
    override def parseDelimitedFrom(inputStream:InputStream):A
    def javaToScala(message:B):A
    override def javaToScala(message:com.google.protobuf.Message):Message = javaToScala(message.asInstanceOf[B])
}
