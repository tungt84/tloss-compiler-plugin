package org.tloss.compiler.soot;

import java.io.IOException;
import java.io.OutputStream;

public interface CompileClassHelper {
	public void compilePreClass(OutputStream outputStream, String className) throws IOException;

	public void compileSubClass(OutputStream outputStream, String className) throws IOException;
}
