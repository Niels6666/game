package opengl;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_INT;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20C.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;

public class VAO {
	int ID;
	VBO indexVBO = null;
	int indexCount;
	
	List<VBO> vbos = new ArrayList<>();
	
	public VAO() {
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pID = stack.mallocInt(1);
			glGenVertexArrays(pID);
			ID = pID.get(0);
		}
		indexVBO = null;
		indexCount = 0;
	}
	public void delete() {
		if(indexVBO != null)
			indexVBO.delete();
		vbos.forEach(VBO::delete);
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pID = stack.ints(ID);
			glDeleteVertexArrays(pID);
		}
	}

	public void bind() {
		glBindVertexArray(ID);
	}
	public void unbind() {
		glBindVertexArray(0);
	}

	public int getIndexCount() {
		return indexCount;
	}

	public VBO getIndexVBO() {
		return indexVBO;
	}

	public List<VBO> getAttributeVBOs() {
		return vbos;
	}

	public void bindAttribute(int attribute)  {
		glEnableVertexAttribArray(attribute);
	}
	public void unbindAttribute(int attribute)  {
		glDisableVertexAttribArray(attribute);
	}

	public void createIndexBuffer(int[] indices) {
		VBO indexVBO = new VBO(GL_ELEMENT_ARRAY_BUFFER);
		indexVBO.bind();
		indexVBO.storeData(indices, GL_STATIC_DRAW);
		//VBO_Unbind(indexVBO); DO NOT UNBIND !!

		this.indexVBO = indexVBO;
		this.indexCount = indices.length;
	}
	public void createFloatAttribute(int attribNumber, float[] data, int size, int stride, int usage) {
		VBO vbo = new VBO(GL_ARRAY_BUFFER);
		vbo.bind();
		vbo.storeData(data, usage);
		glVertexAttribPointer(attribNumber, size, GL_FLOAT, false, stride*4, 0L);
		vbo.unbind();

		vbos.add(vbo);
	}
	public void createIntAttribute(int attribNumber, int[] data, int size, int stride, int usage) {
		VBO vbo = new VBO(GL_ARRAY_BUFFER);
		vbo.bind();
		vbo.storeData(data, usage);
		glVertexAttribPointer(attribNumber, size, GL_INT, false, stride*4, 0L);
		vbo.unbind();

		vbos.add(vbo);
	}

};