package coursier.cli.jvm

import java.io.File

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import coursier.cli.setup.MaybeInstallJvm
import coursier.cli.Util.ValidatedExitOnError
import coursier.env.{EnvironmentUpdate, EnvVarUpdater, ProfileUpdater, WindowsEnvVarUpdater}
import coursier.jvm.{JvmCache, JvmCacheLogger}
import coursier.launcher.internal.Windows
import coursier.util.{Sync, Task}

import scala.concurrent.duration.Duration

object JavaHome extends CaseApp[JavaHomeOptions] {

  def run(options: JavaHomeOptions, args: RemainingArgs): Unit = {

    val params = JavaHomeParams(options).exitOnError()

    val pool                  = Sync.fixedThreadPool(params.cache.parallel)
    val logger                = params.output.logger()
    val coursierCache         = params.cache.cache(pool, logger)
    val noUpdateCoursierCache = params.cache.cache(pool, logger, overrideTtl = Some(Duration.Inf))

    val (jvmCache, javaHome) = params.shared.cacheAndHome(
      coursierCache,
      noUpdateCoursierCache,
      params.repository.repositories,
      params.output.verbosity
    )
    val task = javaHome.getWithRetainedId(params.shared.id)

    val (retainedId, home) = logger.use {
      try task.unsafeRun()(coursierCache.ec) // TODO Better error messages for relevant exceptions
      catch {
        case e: JvmCache.JvmCacheException if params.output.verbosity <= 1 =>
          System.err.println(e.getMessage)
          sys.exit(1)
      }
    }

    lazy val envUpdate = javaHome.environmentFor(retainedId, home)
    if (params.env.env) {
      val script = coursier.jvm.JavaHome.finalScript(envUpdate, jvmCache.baseDirectory.toPath)
      print(script)
    }
    else if (params.env.disableEnv) {
      val script = coursier.jvm.JavaHome.disableScript(jvmCache.baseDirectory.toPath)
      print(script)
    }
    else if (params.env.setup) {
      val setupTask = params.env.setupTask(
        envUpdate,
        params.env.envVarUpdater,
        params.output.verbosity,
        MaybeInstallJvm.headerComment
      )
      setupTask.unsafeRun()(coursierCache.ec)
    }
    else
      println(home.getAbsolutePath)
  }
}
