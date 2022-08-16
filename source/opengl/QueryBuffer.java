package opengl;

import java.util.ArrayList;
import java.util.List;

public class QueryBuffer {
    int type;
    List<Query> buffer = new ArrayList<>();
    long[] lastResult = new long[1];
    final int max_size = 10;

    public QueryBuffer(int type){
        this.type = type;
    }

    public void delete(){
        while(!buffer.isEmpty()){
            buffer.remove(0).delete();
        }
    }

    public int size(){
        return buffer.size();
    }

    public Query push_back(){
        if(size() > max_size){
            buffer.remove(0).delete(); //remove first
        }
        buffer.add(new Query(type));
        return buffer.get(buffer.size()-1);
    }

    public Query last(){
        if(buffer.isEmpty()){
            return null;
        }else{
            return buffer.get(buffer.size()-1);
        }
    }

    public boolean resultAvailable(){
		if(size() > 0){
			return buffer.get(0).resultAvailable();
		}
		return false;
	}

    public long getLastResult(boolean update) {
		if(update){
			while(getResultAndPopFrontIfAvailable(lastResult));
		}
		return lastResult[0];
	}

	public boolean getResultAndPopFrontIfAvailable(long[] res){
		if(buffer.isEmpty()){
			return false;
		}
		if(buffer.get(0).getResultNoWait(res)){
			buffer.remove(0).delete();
			lastResult[0] = res[0];
			return true;
		}
		return false;
	}
}
