import sbt._

class ScalaProtobuf(info: ProjectInfo) extends DefaultProject(info)
{
    override def pomExtra =
        <licenses>
            <license>
                <name>Apache 2</name>
                <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
                <distribution>repo</distribution>
            </license>
        </licenses>
}
