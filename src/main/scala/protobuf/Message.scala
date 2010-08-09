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
import java.io.OutputStream

/**
 * @author jplaisance
 */

object ByteArrayByteStringImplicits {
    implicit def byteArrayToByteString(bytes:Array[Byte]):ByteString = ByteString.copyFrom(bytes)
    implicit def byteStringToByteArray(byteString:ByteString):Array[Byte] = byteString.toByteArray
}

trait Message {
    def get(i:Int):Any
    def write(outputStream:OutputStream):Unit
    def javaMessage:com.google.protobuf.Message
}

trait MessageBuilder {
    def set(i:Int, fieldValue:Option[Any]):Unit
}
