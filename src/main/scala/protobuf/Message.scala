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
    type A <: TypedMessage[A,B]
    type B <: com.google.protobuf.Message
    def get(i:Int):Any
    def writeTo(outputStream:OutputStream):Unit
    def writeDelimitedTo(outputStream:OutputStream):Unit
    def javaMessage:B
    def copyAndSet(i:Int, fieldValue:Any):A
}

trait TypedMessage[C <: TypedMessage[C, D], D <: com.google.protobuf.Message] extends Message {
    type A = C
    type B = D
}

trait MessageBuilder {
    type A <: TypedMessage[A, B]
    type B <: com.google.protobuf.Message
    def set(i:Int, fieldValue:Any):Unit
    def build:A
}

trait TypedMessageBuilder[C <: TypedMessage[C, D], D <: com.google.protobuf.Message] extends MessageBuilder {
    type A = C
    type B = D
}

trait MessageParser {
    type A <: TypedMessage[A, B]
    type B <: com.google.protobuf.Message
    def parseFrom(inputStream:InputStream):A
    def parseDelimitedFrom(inputStream:InputStream):A
    def javaToScala(b:B):A
}

trait TypedMessageParser[C <: TypedMessage[C, D], D <: com.google.protobuf.Message] extends MessageParser {
    type A = C
    type B = D
}
