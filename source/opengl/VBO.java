package opengl;

import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

public class VBO {
	int ID;
	int type;
	int dataLength;
	
	public VBO(int type) {
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pID = stack.mallocInt(1); // int*
			glGenBuffers(pID);
			ID = pID.get(0);
		}
		this.type = type;
	}
	public void delete(){
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pID = stack.ints(ID);
			glDeleteBuffers(pID);
		}
	}

	public int getDataLength(){
		return dataLength;
	}

	public void bind() {
		glBindBuffer(type, ID);
	}
	public void unbind() {
		glBindBuffer(type, 0);
	}

	public void reserveData(long size, int usage) {
		glBufferData(type, size, usage);
		dataLength = (int)size;
	}
	public void storeData(ByteBuffer data, int usage) {
		glBufferData(type, data, usage);
		dataLength = data.capacity();
	}
	public void storeData(float[] data, int usage) {
		glBufferData(type, data, usage);
		dataLength = data.length * 4;
	}
	public void updateData(float[] data) {
		glBufferSubData(type, 0, data);
	}
	public void storeData(int[] data, int usage) {
		glBufferData(type, data, usage);
		dataLength = data.length * 4;
	}
	public void updateData(int[] data) {
		glBufferSubData(type, 0, data);
	}

	public int setType(int type){
		int oldType = this.type;
		this.type = type;
		return oldType;
	}

	public int getID() {
		return ID;
	}

}
