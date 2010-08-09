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

package protobuf.compiler
import google.protobuf.compiler.Plugin._
import collection.JavaConversions
import google.protobuf.compiler.Plugin.CodeGeneratorResponse.File
import collection.mutable.{ListBuffer, LinkedHashMap}
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.{FieldDescriptor, Descriptor, FileDescriptor}
import java.io.{StringReader, BufferedReader, StringWriter, PrintWriter}

/**
 * @author jplaisance
 */

object ScalaProtoWrapperGenerator {
    import JavaConversions.asIterator
    def main(args: Array[String]) {
        val request = CodeGeneratorRequest.parseFrom(System.in)
        val protoFiles = request.getProtoFileList
        val builtDeps = new LinkedHashMap[String, FileDescriptor]
        protoFiles.iterator.foreach(protoFile => {
            val deps = protoFile.getDependencyList
            val descriptors = Array.newBuilder[FileDescriptor]
            deps.iterator.foreach(dep => {
                descriptors+=builtDeps.get(dep).get
            })
            builtDeps.put(protoFile.getName, FileDescriptor.buildFrom(protoFile, descriptors.result))
        })
        val builder = CodeGeneratorResponse.newBuilder
        for (fileDescriptor <- builtDeps.values) {
            builder.addFile(File.newBuilder().setName(fileDescriptor.getPackage.replaceAll("\\.", "/")+"/"+fileDescriptor.getOptions.getJavaOuterClassname+".scala").setContent(generateForFileDescriptor(fileDescriptor)))
        }
        builder.build.writeTo(System.out)
    }

    def generateForFileDescriptor(fileDescriptor:FileDescriptor):String = {
        val stringWriter = new StringWriter()
        val out = new PrintWriter(stringWriter)
        out.println("package "+fileDescriptor.getPackage)
        val options = fileDescriptor.getOptions
        val javaClass = options.getJavaOuterClassname
        out.println("import "+options.getJavaPackage+"."+javaClass)
        out.println("import protobuf.TypedMessage")
        out.println("import protobuf.TypedMessageBuilder")
        out.println("import collection.mutable.ListBuffer")
        out.println("import java.io.{InputStream, OutputStream}")
        out.println("import collection.JavaConversions")
        out.println("import com.google.protobuf.ByteString")
        out.println
        fileDescriptor.getMessageTypes.iterator.foreach(messageType => {
            out.print(makeClassesForDescriptor(messageType, javaClass))
        })
        stringWriter.toString
    }

    def makeClassesForDescriptor(descriptor:Descriptor, javaClass:String):String = {
        val stringWriter = new StringWriter()
        val out = new PrintWriter(stringWriter)
        descriptor.getNestedTypes.iterator.foreach(x => out.print(indentString(makeClassesForDescriptor(x, javaClass))))
        val fields = descriptor.getFields.iterator.toList

        val requiredFields = fields.filter(field => field.isRequired)
        val requiredFieldTypes = getFieldTypes(requiredFields, javaClass)
        val constructorFields = requiredFields.zip(requiredFieldTypes.unzip._1).map(x => "val "+x._1.getName+":"+x._2)

        val optionalFields = fields.filter(field => field.isOptional)
        val optionalFieldTypes = getFieldTypes(optionalFields, javaClass)
        val optionalFieldVals = optionalFields.zip(optionalFieldTypes.unzip._1).map(x => "val "+x._1.getName+":Option["+x._2+"]")
        val optionalFieldVars = optionalFields.zip(optionalFieldTypes.unzip._1).map(x => "var "+x._1.getName+":Option["+x._2+"] = None")

        val repeatedFields = fields.filter(field => field.isRepeated)
        val repeatedFieldTypes = getFieldTypes(repeatedFields, javaClass)
        val repeatedFieldLists = repeatedFields.zip(repeatedFieldTypes.unzip._1).map(x => "val "+x._1.getName+":List["+x._2+"]")
        val repeatedFieldListBuffers = repeatedFields.zip(repeatedFieldTypes.unzip._1).map(x => "val "+x._1.getName+":ListBuffer["+x._2+"] = new ListBuffer["+x._2+"]")

        val name = descriptor.getName
        val javaSubClass = javaClass+"."+name

        out.print("class "+name+"(")
        val spaces = " "*(name.length+7)
        out.println((constructorFields++optionalFieldVals++repeatedFieldLists).mkString(",\n"+spaces))
        out.println("        ) extends TypedMessage["+javaSubClass+"] {")
        out.println("    def javaMessage:"+javaSubClass+" = {")
        out.println("        val builder = "+javaSubClass+".newBuilder")
        for ((field, isMessage) <- requiredFields.zip(requiredFieldTypes.unzip._2)) {
            val fieldName = field.getName
            out.println("        builder.set"+upcaseFirstLetter(fieldName)+"("+fieldName+(if(isMessage)".javaMessage" else "")+")")
        }
        for ((field, isMessage) <- optionalFields.zip(optionalFieldTypes.unzip._2)) {
            val fieldName = field.getName
            out.println("        "+fieldName+".foreach(x => builder.set"+upcaseFirstLetter(fieldName)+"(x"+(if(isMessage)".javaMessage" else "")+"))")
        }
        for ((field, isMessage) <- repeatedFields.zip(repeatedFieldTypes.unzip._2)) {
            val fieldName = field.getName
            out.println("        "+fieldName+".foreach(x => builder.add"+upcaseFirstLetter(fieldName)+"(x"+(if(isMessage)".javaMessage" else "")+"))")
        }
        out.println("        builder.build")
        out.println("    }")
        out.println
        out.println("    def write(outputStream:OutputStream):Unit = {")
        out.println("        javaMessage.writeDelimitedTo(outputStream)")
        out.println("    }")
        out.println
        out.println("    def get(i:Int):Any = {")
        if (!fields.isEmpty) {
            out.println("        i match {")
            fields.foreach(field => out.println("            case "+field.getNumber+" => "+field.getName))
            out.println("        }")
        }
        out.println("    }")
        out.println("}")
        out.println

        out.println("object "+name+" {")
        out.println
        out.println("    def parse(inputStream:InputStream):"+name+" = {")
        out.println("        val message = "+javaSubClass+".parseDelimitedFrom(inputStream)")
        out.println("        javaToScala(message)")
        out.println("    }")
        out.println
        out.println("    def javaToScala(message:"+javaSubClass+"):"+name+" = {")
        val requiredGetters = new ListBuffer[String]
        for ((field, (typeName, isMessage)) <- requiredFields.zip(requiredFieldTypes)) {
            val fieldName = field.getName
            requiredGetters+=((if (isMessage)typeName+".javaToScala(" else "")+"message.get"+upcaseFirstLetter(fieldName)+"()"+(if (isMessage) ")" else ""))
        }
        val optionalGetters = new ListBuffer[String]
        for ((field, (typeName, isMessage)) <- optionalFields.zip(optionalFieldTypes)) {
            val fieldName = field.getName
            val upcase = upcaseFirstLetter(fieldName)
            optionalGetters+=("(if (message.has"+upcase+"()) Some("+(if (isMessage) typeName+".javaToScala(" else "")+"message.get"+upcase+"())"+(if (isMessage) ")" else "")+" else None)")
        }
        val repeatedGetters = new ListBuffer[String]
        for ((field, (typeName, isMessage)) <- repeatedFields.zip(repeatedFieldTypes)) {
            val fieldName = field.getName
            repeatedGetters+=("JavaConversions.asIterator(message.get"+upcaseFirstLetter(fieldName)+"List().iterator)"+(if (isMessage) ".map(x => "+typeName+".javaToScala(x))" else "")+".toList")
        }
        val spaces2 = " "*(name.length+13)
        out.println("        new "+name+"("+(requiredGetters++optionalGetters++repeatedGetters).mkString(",\n"+spaces2)+"\n        )")
        out.println("    }")
        out.println("}")
        out.println

        out.print("class "+name+"Builder(")
        out.print(constructorFields.mkString(","))
        out.println(") extends TypedMessageBuilder["+name+", "+javaSubClass+"] {")
        for (field <- optionalFieldVars) {
            out.println("    "+field)
        }
        out.println
        for (field <- repeatedFieldListBuffers) {
            out.println("    "+field)
        }
        out.println
        out.println("    def set(i:Int, fieldValue:Option[Any]):Unit = {")
        if (!optionalFields.isEmpty) {
            out.println("        i match {")
            optionalFields.foreach(field => out.println("            case "+field.getNumber+" => "+field.getName+" = fieldValue.asInstanceOf[Option["+getTypeString(field, javaClass)._1+"]]"))
            out.println("        }")
        }
        out.println("    }")
        out.println
        out.println("    def build:"+name+" = {")
        out.println("        new "+name+"("+(requiredFields.map(x => x.getName())++optionalFields.map(x => x.getName())++repeatedFields.map(x => x.getName()+".result")).mkString(",\n"+spaces2)+"\n        )")
        out.println("    }")
        out.println("}")
        out.println
        stringWriter.toString
    }

    def indentString(str:String):String = {
        val reader = new BufferedReader(new StringReader(str))
        var line = reader.readLine
        val stringWriter = new StringWriter()
        val out = new PrintWriter(stringWriter)
        while (line != null) {
            out.println("    "+line)
            line = reader.readLine
        }
        stringWriter.toString
    }

    def getFieldTypes(fields:List[FieldDescriptor], javaClass:String):List[(String, Boolean)] = {
        fields.map(field => getTypeString(field, javaClass))
    }

    def getTypeString(field:FieldDescriptor, javaClass:String):(String,Boolean) = {
        def getContainingType(descriptor:Descriptor):String = {
            if (descriptor != null) getContainingType(descriptor.getContainingType)+descriptor.getName+"." else ""
        }
        field.getJavaType match {
            case JavaType.BOOLEAN => ("Boolean", false)
            case JavaType.BYTE_STRING => ("ByteString", false)
            case JavaType.DOUBLE => ("Double", false)
            case JavaType.ENUM =>
                val enumType = field.getEnumType
                (javaClass+"."+getContainingType(enumType.getContainingType)+enumType.getName, false)
            case JavaType.FLOAT => ("Float", false)
            case JavaType.INT => ("Int", false)
            case JavaType.LONG => ("Long", false)
            case JavaType.MESSAGE =>
                val mType = field.getMessageType
                (getContainingType(mType.getContainingType)+mType.getName, true)
            case JavaType.STRING => ("String", false)
        }
    }

    def upcaseFirstLetter(str:String):String = str.charAt(0).toUpper+(if (str.length > 1) str.substring(1, str.length) else "")
}
