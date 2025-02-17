package coursier.cli.jvm

import caseapp.Recurse
import coursier.cli.options.RepositoryOptions

// format: off
final case class SharedJavaOptions(
  jvm: Option[String] = None,
  jvmDir: Option[String] = None,
  systemJvm: Option[Boolean] = None,
  localOnly: Boolean = false,
  update: Boolean = false,
  jvmIndex: Option[String] = None,
  @Recurse
    repositoryOptions: RepositoryOptions = RepositoryOptions()
)
// format: on
