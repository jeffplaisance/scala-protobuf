import sbt._

class ScalaProtobuf(info: ProjectInfo) extends DefaultProject(info)
{
    override def mainClass = Some("protobuf.compiler.ScalaProtoWrapperGenerator")
}
