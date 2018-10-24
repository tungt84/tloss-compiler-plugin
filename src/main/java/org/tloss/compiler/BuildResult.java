package org.tloss.compiler;

public class BuildResult {
	public static enum Result {
		BUILD_FAILED, BUILD_OK
	}

	private String classFolder;
	private String classPath;
	private Result result;

	public String getClassFolder() {
		return classFolder;
	}
	

	public String getClassPath() {
		return classPath;
	}


	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}


	public void setClassFolder(String classFolder) {
		this.classFolder = classFolder;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

}
