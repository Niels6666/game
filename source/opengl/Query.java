package opengl;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.*;


public class Query {
    int id;
    int type;
    long s;

    public Query(int type) {
        this.type = type;

        try ( MemoryStack stack = stackPush() ) {
			IntBuffer pID = stack.mallocInt(1);
			GL46C.glGenQueries(pID);
			id = pID.get(0);
		}
    }

    public void delete(){
        GL46C.glDeleteQueries(id);
        GL46C.glDeleteSync(s);
    }

    public void begin(){
        GL46C.glBeginQuery(type, id);
    }

    public void end(){
        GL46C.glEndQuery(type);
        s = GL46C.glFenceSync(GL46C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

	/**
	 * Only if the query is GL_TIMESTAMP
	 */
    public void queryCounter(){
        GL46C.glQueryCounter(id, type);
		s = GL46C.glFenceSync(GL46C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    boolean resultAvailable(){
		return GL46C.glClientWaitSync(s, GL46C.GL_SYNC_FLUSH_COMMANDS_BIT, 0) == GL46C.GL_ALREADY_SIGNALED;
	}

	boolean getResultNoWait(long[] res){
		if(!resultAvailable()){
			return false;
		}

		int status = GL46C.GL_FALSE;
		try ( MemoryStack stack = stackPush() ) {
			long pointer = stack.getPointerAddress();
			IntBuffer p = stack.mallocInt(1);
			GL46C.glGetQueryObjectiv(id, GL46C.GL_QUERY_RESULT_AVAILABLE, pointer);
			status = p.get(0);
		}


		if(status == GL46C.GL_TRUE){
			res[0] = getResult();
		}
		return true;
	}

	long getResult(){
		try ( MemoryStack stack = stackPush() ) {
			LongBuffer p = stack.mallocLong(1);
			long pointer = stack.getPointerAddress();
			GL46C.glGetQueryObjecti64v(id, GL46C.GL_QUERY_RESULT, pointer);
			return p.get(0);
		}
	}
}
