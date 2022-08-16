package opengl;

import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

public class Shader {

	int programID;
	int computeShaderID = -1;
	int vertexShaderID = -1;
	int tessControlShaderID = -1;
	int tessEvaluationShaderID = -1;
	int geometryShaderID = -1;
	int fragmentShaderID = -1;
	Map<String, Integer> uniforms = new HashMap<>();

	public Shader(String computeFilePath) {
		vertexShaderID = loadFromFile(computeFilePath, GL_COMPUTE_SHADER);
		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
	}

	public Shader(ArrayList<String> vertexShader, ArrayList<String> fragmentShader) {
		vertexShaderID = loadFromFile(vertexShader, GL_VERTEX_SHADER, "*no file path*");
		geometryShaderID = -1;
		fragmentShaderID = loadFromFile(fragmentShader, GL_FRAGMENT_SHADER, "*no file path*");

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	/**
	 * Construit un nouveau shader. <br>
	 * Ne pas oublier d'appeler:<br>
	 * -Shader::bindVertexAttribute ***<br>
	 * -Shader::bindFragDataLocation ***<br>
	 * -Shader::finishInit<br>
	 * -Shader::getUniformLocation ***<br>
	 * -Shader::start<br>
	 * -Shader::connectTextureUnit ***<br>
	 * -Shader::stop<br>
	 * de façon à finaliser l'initialisation du shader.<br>
	 * les fonctions notées *** sont optionnelles.
	 * 
	 * @param vertexFilePath
	 * @param fragmentFilePath
	 */
	public Shader(String vertexFilePath, String fragmentFilePath) {
		vertexShaderID = loadFromFile(vertexFilePath, GL_VERTEX_SHADER);
		geometryShaderID = -1;
		fragmentShaderID = loadFromFile(fragmentFilePath, GL_FRAGMENT_SHADER);

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	public Shader(String vertexFilePath, String geometryFilePath, String fragmentFilePath) {
		vertexShaderID = loadFromFile(vertexFilePath, GL_VERTEX_SHADER);
		geometryShaderID = loadFromFile(geometryFilePath, GL_GEOMETRY_SHADER);
		fragmentShaderID = loadFromFile(fragmentFilePath, GL_FRAGMENT_SHADER);

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, geometryShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	public Shader(String computeFile, int dummy) {
		vertexShaderID = loadFromSource(computeFile, GL_COMPUTE_SHADER);
		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
	}

	public Shader(String vertexFile, String fragmentFile, int dummy) {
		vertexShaderID = loadFromSource(vertexFile, GL_VERTEX_SHADER);
		geometryShaderID = -1;
		fragmentShaderID = loadFromSource(fragmentFile, GL_FRAGMENT_SHADER);

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	public Shader(String vertexFile, String geometryFile, String fragmentFile, int dummy) {
		vertexShaderID = loadFromSource(vertexFile, GL_VERTEX_SHADER);
		geometryShaderID = loadFromSource(geometryFile, GL_GEOMETRY_SHADER);
		fragmentShaderID = loadFromSource(fragmentFile, GL_FRAGMENT_SHADER);

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, geometryShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	public Shader(String vertexFile, String tessellationControlFile, String tessellationEvaluationFile,
			String geometryFile, String fragmentFile) {
		vertexShaderID = loadFromSource(vertexFile, GL_VERTEX_SHADER);
		tessControlShaderID = loadFromSource(tessellationControlFile, GL_TESS_CONTROL_SHADER);
		tessEvaluationShaderID = loadFromSource(tessellationEvaluationFile, GL_TESS_EVALUATION_SHADER);
		geometryShaderID = loadFromSource(geometryFile, GL_GEOMETRY_SHADER);
		fragmentShaderID = loadFromSource(fragmentFile, GL_FRAGMENT_SHADER);

		programID = glCreateProgram();

		glAttachShader(programID, vertexShaderID);
		glAttachShader(programID, tessControlShaderID);
		glAttachShader(programID, tessEvaluationShaderID);
		glAttachShader(programID, geometryShaderID);
		glAttachShader(programID, fragmentShaderID);
	}

	public void delete() {
		glUseProgram(0);
		if (computeShaderID != -1)
			glDetachShader(programID, computeShaderID);
		if (vertexShaderID != -1)
			glDetachShader(programID, vertexShaderID);
		if (tessControlShaderID != -1)
			glDetachShader(programID, tessControlShaderID);
		if (tessEvaluationShaderID != -1)
			glDetachShader(programID, tessEvaluationShaderID);
		if (geometryShaderID != -1)
			glDetachShader(programID, geometryShaderID);
		if (fragmentShaderID != -1)
			glDetachShader(programID, fragmentShaderID);

		if (computeShaderID != -1)
			glDeleteShader(computeShaderID);
		if (vertexShaderID != -1)
			glDeleteShader(vertexShaderID);
		if (tessControlShaderID != -1)
			glDeleteShader(tessControlShaderID);
		if (tessEvaluationShaderID != -1)
			glDeleteShader(tessEvaluationShaderID);
		if (geometryShaderID != -1)
			glDeleteShader(geometryShaderID);
		if (fragmentShaderID != -1)
			glDeleteShader(fragmentShaderID);

		glDeleteProgram(programID);
	}

	public void start() {
		glUseProgram(programID);
	}

	public void stop() {
		glUseProgram(0);
	}

	public void finishInit() {
		glLinkProgram(programID);
		int linkRes = 0;
		try (MemoryStack stack = stackPush()) {
			IntBuffer pLinkRes = stack.mallocInt(1);
			glGetProgramiv(programID, GL_LINK_STATUS, pLinkRes);
			linkRes = pLinkRes.get(0);
		}

		if (linkRes == GL_FALSE) {
			System.err.println("Error while linking shader:");
			int sizeNeeded = 0;
			try (MemoryStack stack = stackPush()) {
				IntBuffer pSizeNeeded = stack.mallocInt(1);
				glGetProgramiv(programID, GL_INFO_LOG_LENGTH, pSizeNeeded);
				sizeNeeded = pSizeNeeded.get(0);

				ByteBuffer strBuff = stack.calloc(sizeNeeded);
				glGetProgramInfoLog(programID, pSizeNeeded, strBuff);

				String errMsg = StandardCharsets.UTF_8.decode(strBuff).toString();
				System.err.println(errMsg);
			}

			throw new IllegalArgumentException("Shader compile error");
		} else {
			System.out.println("GLSL program linked successfully");
		}
		glValidateProgram(programID);
	}

	public void bindVertexAttribute(int attribute, String variableName) {
		glBindAttribLocation(programID, attribute, variableName);
	}

	public void bindFragDataLocation(int colorAttachment, String variableName) {
		glBindFragDataLocation(programID, colorAttachment, variableName);
	}

	public void connectTextureUnit(String sampler_name, int value) {
		loadInt(sampler_name, value);
	}

	public void loadInt(String name, int value) {
		glUniform1i(findUniformLoc(name), value);
	}

	public void loadUInt(String name, int value) {
		glUniform1ui(findUniformLoc(name), value);
	}

	public void loadFloat(String name, float value) {
		glUniform1f(findUniformLoc(name), value);
	}

	public void loadVec2(String name, Vector2fc v) {
		glUniform2f(findUniformLoc(name), v.x(), v.y());
	}

	public void loadiVec2(String name, Vector2ic v) {
		glUniform2i(findUniformLoc(name), v.x(), v.y());
	}

	public void loadVec3(String name, Vector3fc v) {
		glUniform3f(findUniformLoc(name), v.x(), v.y(), v.z());
	}

	public void loadVec4(String name, Vector4fc v) {
		glUniform4f(findUniformLoc(name), v.x(), v.y(), v.z(), v.w());
	}

	public void loadMat4(String name, Matrix4fc mat) {
		try (MemoryStack stack = stackPush()) {
			FloatBuffer buffer = stack.mallocFloat(16);
			mat.get(buffer);
			glUniformMatrix4fv(findUniformLoc(name), false, buffer);
		}
	}

	public int get(String name) {
		return uniforms.get(name);
	}

	public void init_uniforms(List<String> names) {
		start();
		for (String name : names) {
			int loc = getUniformLocation(name);

			if (loc == -1) {
				System.out.println("Uniform location of " + name + " = " + loc);
				System.out.println(
						" 	--> The uniform variable name is either incorrect or the uniform variable is not used");
				uniforms.put(name, loc);
			} else {
				uniforms.put(name, loc);
			}

		}
		stop();
	}

	private int loadFromFile(String filePath, int programType) {
		ArrayList<String> lines = null;
		try {
			lines = (ArrayList<String>) Files.readAllLines(new File(filePath).toPath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		return loadFromFile(lines, programType, filePath);
	}

	private int loadFromFile(ArrayList<String> lines, int programType, String filePath) {

		String source = "";
		for (String s : lines) {
			source += s + "\n";
		}

		int shaderID = glCreateShader(programType);
		glShaderSource(shaderID, source);
		glCompileShader(shaderID);

		try (MemoryStack stack = stackPush()) {
			IntBuffer pStatus = stack.mallocInt(1);
			glGetShaderiv(shaderID, GL_COMPILE_STATUS, pStatus);
			int status = pStatus.get(0);

			if (status == GL_FALSE) {
				IntBuffer pSizeNeeded = stack.mallocInt(1);
				glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, pSizeNeeded);
				ByteBuffer strBuff = stack.calloc(pSizeNeeded.get(0));
				glGetShaderInfoLog(shaderID, pSizeNeeded, strBuff);
				String errMsg = StandardCharsets.UTF_8.decode(strBuff).toString();

				System.err.println("Erreur lors de la compilation de " + filePath + " :");
				System.err.println(errMsg);
				throw new IllegalArgumentException("Shader compile error");
			}
		}

		return shaderID;
	}

	private int loadFromSource(String file, int programType) {
		int shaderID = glCreateShader(programType);
		glShaderSource(shaderID, file);
		glCompileShader(shaderID);

		try (MemoryStack stack = stackPush()) {
			IntBuffer pStatus = stack.mallocInt(1);
			glGetShaderiv(shaderID, GL_COMPILE_STATUS, pStatus);
			int status = pStatus.get(0);

			if (status == GL_FALSE) {
				IntBuffer pSizeNeeded = stack.mallocInt(1);
				ByteBuffer strBuff = stack.calloc(pSizeNeeded.get(0));
				glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, pSizeNeeded);
				glGetShaderInfoLog(shaderID, pSizeNeeded, strBuff);
				String errMsg = StandardCharsets.UTF_8.decode(strBuff).toString();

				System.err.println("Erreur lors de la compilation d'un shader:");
				System.err.println(errMsg);
				throw new IllegalArgumentException("Shader compile error");
			}
		}

		return shaderID;
	}

	private int getUniformLocation(String variableName) {
		return glGetUniformLocation(programID, variableName);
	}

	private int findUniformLoc(String name) {
		Integer loc = uniforms.get(name);
		if (loc == null) {
			throw new IllegalArgumentException("Error, unknown uniform variable name: " + name);
		}
		return loc;
	}

};