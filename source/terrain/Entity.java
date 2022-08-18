package terrain;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.Game;
import engine.Window;
import opengl.Texture;

public abstract class Entity {

    public enum Orientation{
        FRONT, LEFT, RIGHT;
    }

	protected static BVH<Entity> bvh = new BVH<>();

    protected Vector2f position;
    protected Vector2f halfSize;
	protected BVHNode<Entity> node;

    protected Entity(Vector2f position, Vector2f halfSize){
        this.position = position;
        this.halfSize = halfSize;

		this.node = new BVHNode<Entity>(new AABB(position, Math.max(halfSize.x, halfSize.y)), this);
		bvh.add(node);
    }

    public Vector2f getPosition(){
        return position;
    }
    
    abstract Matrix4f getTransform();
    abstract Texture getTexture();
    abstract Texture getGlowTexture();

    protected abstract boolean internalUpdate(World w, Game game, Window window);
    protected abstract void internalDelete();
    
	public void update(World w, Game game, Window window) {
        boolean updateBVH = internalUpdate(w, game, window);
        if(updateBVH){
		    node.box.set(position, Math.max(halfSize.x, halfSize.y));
		    bvh.update(node);
        }
	}

	public void delete() {
        internalDelete();
		bvh.remove(node);
	}
	
}
