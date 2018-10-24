package org.tloss.compiler.maven;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.tloss.compiler.BuildResult;
import org.tloss.compiler.BuildResult.Result;
import org.tloss.compiler.JavaCompilerHandler;

public class MavenCompilerHandler implements JavaCompilerHandler {
	private String pomFile;
	private String mavenHome;

	public void init(Properties properties) throws Exception {
		if (properties.containsKey("MavenCompilerHandler.pomFile")) {
			pomFile = properties.getProperty("MavenCompilerHandler.pomFile");
			if (!new File(pomFile).isFile()) {
				throw new Exception("MavenCompilerHandler.pomFile:" + pomFile + " must be a file!!!");
			}
		} else {
			throw new Exception("MavenCompilerHandler.pomFile is not found in config file!!!!");
		}
		if (properties.containsKey("MavenCompilerHandler.maven.home")) {
			mavenHome = properties.getProperty("MavenCompilerHandler.maven.home");
			if (!new File(mavenHome).isDirectory()) {
				throw new Exception("MavenCompilerHandler.maven.home:" + mavenHome + " must be a folder!!!");
			}
		} else {
			throw new Exception("MavenCompilerHandler.maven.home is not found in config file!!!!");
		}
	}

	public BuildResult compile() throws Exception {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(pomFile));
		List<String> goals = new ArrayList<String>();
		goals.add("dependency:copy-dependencies");
		goals.add("compile");
		request.setGoals(goals);

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(mavenHome));
		InvocationResult result = invoker.execute(request);
		BuildResult buildResult = new BuildResult();
		if (result.getExitCode() != 0) {
			buildResult.setResult(Result.BUILD_FAILED);
		} else {
			buildResult.setResult(Result.BUILD_OK);
			File target = new File(request.getPomFile().getParentFile(), "target");
			buildResult.setClassFolder(new File(target, "classes").getAbsolutePath());
			File dependency = new File(target, "dependency");
			final StringBuffer buffer = new StringBuffer();
			buffer.append(buildResult.getClassFolder());
			if (dependency.exists()) {
				dependency.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						if (pathname.isFile() && pathname.getName().endsWith(".jar")) {
							buffer.append(File.pathSeparator).append(pathname.getAbsolutePath());
							return true;
						}
						return false;
					}
				});
			}
			buildResult.setClassPath(buffer.toString());
		}
		return buildResult;
	}

}
