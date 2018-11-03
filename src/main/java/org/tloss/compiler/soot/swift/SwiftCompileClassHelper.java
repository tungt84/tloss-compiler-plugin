package org.tloss.compiler.soot.swift;

import java.io.IOException;
import java.io.OutputStream;

import org.tloss.compiler.soot.CompileClassHelper;

public class SwiftCompileClassHelper implements CompileClassHelper {

	public void compilePreClass(OutputStream outputStream, String className) throws IOException {
		outputStream.write(("class " + className.replace('.', '_') + "{\n").getBytes("utf-8"));
	}

	public void compileSubClass(OutputStream outputStream, String className) throws IOException {
		outputStream.write("}\n".getBytes("utf-8"));

	}

}
