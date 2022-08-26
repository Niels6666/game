package opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/*

//header

class FBO {
public:
	static void blitFBOs(const FBO* src, glm::vec4 srcRegion, std::initializer_list<std::string> readColorAttachments, const FBO* dest,
			glm::vec4 destRegion, std::initializer_list<std::string> drawColorAttachments, int mask, bool pixelSizes);

	FBO(std::string name, int MULTISAMPLE_COUNT = 0);
	FBO(std::string name, int width, int height, int MULTISAMPLE_COUNT = 0);
	virtual ~FBO();

	GLint getID() {
		return ID;
	}

	int getWidth() const{
		return width;
	}
	int getHeight() const{
		return height;
	}
	int getMultisampleCount() const{
		return MULTISAMPLE_COUNT;
	}
	glm::vec4 getRegion() const{
		return glm::vec4(0, 0, width, height);
	}
	glm::vec4 getNDCRegion() const{
		return glm::vec4(0, 0, 1, 1);
	}
	const std::string& getName() const{
		return name;
	}

	void bind();
	void unbind();
	void setViewport();
	void resize(int width, int height);

	void addTextureAttachment(const std::string& name,
			const AttachmentFormat& format);

	void createRenderBufferAttachement(const std::string& name,
			const AttachmentFormat& format);

	void bindColorAttachments(
			std::initializer_list<std::string> colorAttachments);

	void bindNoColorBuffers();

	bool finish();

	void clearColorAttachment(const std::string& attachmentName,
			glm::vec4 color);
	void clearDepthAttachment(float value);
	void clearStencilAttachment(int value);

	const FBOAttachment* getAttachment(const std::string& attachmentName) const{
		const auto it = attachments.find(attachmentName);
		if (it == end(attachments)) {
			std::string allAttachmentNames = "";
			for(const auto&[name, attachment] : attachments){
				allAttachmentNames += name + " ";
			}
			throw "Error, unknown FBO attachment name: "
					+ attachmentName + " for fbo " + name + ", all attachments: " + allAttachmentNames;
		} else {
			return it->second;
		}
	}

private:
	FBO& operator=(const FBO& fbo) = delete;
	FBO(const FBO& fbo) = delete;
	FBO(FBO&& fbo) = delete;
	void operator=(FBO&& fbo) = delete;

	GLuint ID;
	int width;
	int height;
	int MULTISAMPLE_COUNT;
	std::string name;

	int colorAttachments = 0;
	std::map<std::string, FBOAttachment*> attachments;
	std::vector<GLenum> drawBuffers; //the color attachments which are currently bound

};


///////////// .cpp


**
 * Copy a rectangle of pixels from src to dest. If the sizes don't match,
 * the src rectangle is stretched to fit the dest rectangle.
 * src or dest can be NULL to indicate the default FBO
 *
 * @param src
 *            The first fbo from which values will be copied.
 * @param srcRegion
 *            The src rectangle region in (X0, Y0, X1, Y1) format.
 * @param readColorAttachments
 * 			  The name of the color attachment from src that will be read from.
 * 			  It is an error to specify more than one read buffer.
 * 			  Must be empty if src is NULL or if GL_COLOR_BUFFER_BIT is not given as a mask flag.
 * @param dest
 *            The dest fbo which will receive the pixels
 * @param destRegion
 *            The dest rectangle region in (X0, Y0, X1, Y1) format.
 * @param drawColorAttachments
 * 			  The names of the color attachments from dest that will be drawn to.
 * 			  More than one draw buffer can be specified.
 * 			  Note that the formats must correspond with that of the read buffer.
 * 			  Must be empty if dest is NULL or if GL_COLOR_BUFFER_BIT is not given as a mask flag.
 * @param mask
 *            Which buffers are to be copied: GL_COLOR_BUFFER_BIT |
 *            GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT
 * @param pixelSizes
 *            true if the regions are expressed in pixels, false if they are
 *            expressed in relative coords (i.e [0.0, 1.0]).
 *
void FBO::blitFBOs(const FBO* src, glm::vec4 srcRegion, std::initializer_list<std::string> readColorAttachments, const FBO* dest,
		glm::vec4 destRegion, std::initializer_list<std::string> drawColorAttachments, int mask, bool pixelSizes) {

	if (src != NULL) {
		glBindFramebuffer(GL_READ_FRAMEBUFFER, src->ID);
		if((mask & GL_COLOR_BUFFER_BIT) != 0){
			if(readColorAttachments.size() != 1){
				std::string msg = std::string("More than one color attachment has been given to read from in blitFBOs: ");
				for(const auto& s : readColorAttachments){
					msg += s + " ";
				}
				throw msg;
			}
			const FBOAttachment *attachment = src->getAttachment(*readColorAttachments.begin());
			if (attachment->getFormat().isColorAttachment) {
				glReadBuffer(attachment->getColorAttachmentNumber());
			} else {
				throw "Error, " + *readColorAttachments.begin() + " isn't a color attachment for the read framebuffer";
			}
		}else{
			if(readColorAttachments.size() != 0){
				throw std::string("Error, cannot specify a src color attachment in blitFBOs without specifying the flag GL_COLOR_BUFFER_BIT");
			}
		}
	} else {
		if(readColorAttachments.size() != 0){
			throw std::string("Error, cannot specify a src color attachment in blitFBOs for the default framebuffer");
		}

		glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
		glReadBuffer(GL_BACK);
	}

	if (dest != NULL) {
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dest->ID);

		if((mask & GL_COLOR_BUFFER_BIT) != 0){
			if(drawColorAttachments.size() == 0){
				throw std::string("Error, at least one color attachment must be given to draw to if the flag GL_COLOR_BUFFER_BIT is given and the dest fbo is not the default framebuffer.");
			}

			for (auto &attachmentName : drawColorAttachments) {
				const FBOAttachment *attachment = dest->getAttachment(attachmentName);
				if (attachment->getFormat().isColorAttachment) {
					glDrawBuffer(attachment->getColorAttachmentNumber());
				} else {
					throw "Error, " + attachmentName + " isn't a color attachment for the draw framebuffer";
				}
			}

		}else{
			if(drawColorAttachments.size() != 0){
				throw std::string("Error, cannot specify a dest color attachment in blitFBOs without specifying the flag GL_COLOR_BUFFER_BIT");
			}
		}



	} else {
		if(drawColorAttachments.size() != 0){
			throw std::string("Error, cannot specify a dest color attachment in blitFBOs for the default framebuffer");
		}

		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
		glDrawBuffer(GL_BACK);
	}

	int srcX0, srcY0, srcX1, srcY1, destX0, destY0, destX1, destY1;

	if (pixelSizes) {

		srcX0 = (int) srcRegion.x;
		srcY0 = (int) srcRegion.y;
		srcX1 = (int) srcRegion.z;
		srcY1 = (int) srcRegion.w;

		destX0 = (int) destRegion.x;
		destY0 = (int) destRegion.y;
		destX1 = (int) destRegion.z;
		destY1 = (int) destRegion.w;

	} else {

		if (src == NULL || dest == NULL) {
			throw "Cannot use fract sizes when bliting FBOs when src or dest is NULL";
		}

		srcX0 = (int) srcRegion.x * src->width;
		srcY0 = (int) srcRegion.y * src->height;
		srcX1 = (int) srcRegion.z * src->width;
		srcY1 = (int) srcRegion.w * src->height;

		destX0 = (int) destRegion.x * dest->width;
		destY0 = (int) destRegion.y * dest->height;
		destX1 = (int) destRegion.z * dest->width;
		destY1 = (int) destRegion.w * dest->height;
	}

	int filter = GL_LINEAR;
	if ((mask & GL_DEPTH_BUFFER_BIT) != 0
			|| (mask & GL_STENCIL_BUFFER_BIT) != 0) {
		filter = GL_NEAREST;
	}

	glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, destX0, destY0, destX1,
			destY1, mask, filter);

	//Reset
	glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
	glReadBuffer(GL_BACK);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
	glDrawBuffer(GL_BACK);
}

**
 * Creates a new render buffer attchment. Rendering to a render buffer is
 * faster than rendering to a texture but you can't sample from a render
 * buffer.
 * @param name
 * @param format
 *
public void createRenderBufferAttachement(String name,
		AttachmentFormat format) {
	bind();

	GLuint renderBufferID;
	glGenRenderbuffers(1, &renderBufferID);
	glBindRenderbuffer(GL_RENDERBUFFER, renderBufferID);

	glRenderbufferStorage(GL_RENDERBUFFER, format.internalFormat, width,
			height);

	int colorAttachmentNumber = -1;
	if (format.isColorAttachment) {
		colorAttachmentNumber = this->colorAttachments + format.attachment;

		glFramebufferRenderbuffer(GL_FRAMEBUFFER, colorAttachmentNumber,
		GL_RENDERBUFFER, renderBufferID);

		this->colorAttachments++;
	} else {

		glFramebufferRenderbuffer(GL_FRAMEBUFFER, format.attachment,
		GL_RENDERBUFFER, renderBufferID);

	}

	FBOAttachment *attachment = new FBOAttachment(renderBufferID, false, MULTISAMPLE_COUNT == 0, format,
			name, colorAttachmentNumber, -1);
	attachments[name] = attachment;

	glBindRenderbuffer(GL_RENDERBUFFER, 0);
	unbind();
}



*/

public class FBO {

	public enum AttachmentFormat {

		RGB(GL46C.GL_RGB, GL46C.GL_RGB, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),
		RGB16F(GL46C.GL_RGB16F, GL46C.GL_RGB, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),
		RGB32F(GL46C.GL_RGB32F, GL46C.GL_RGB, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),

		R11F_G11F_B10F(GL46C.GL_R11F_G11F_B10F, GL46C.GL_RGB, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),

		RGBA(GL46C.GL_RGBA8, GL46C.GL_RGBA, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),
		RGBA16F(GL46C.GL_RGBA16F, GL46C.GL_RGBA, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),
		RGBA32F(GL46C.GL_RGBA32F, GL46C.GL_RGBA, GL46C.GL_COLOR_ATTACHMENT0, true, GL46C.GL_FLOAT),

		// warning doesn't work
		DEPTH_COMPONENT(GL46C.GL_DEPTH_COMPONENT, GL46C.GL_DEPTH_COMPONENT, GL46C.GL_DEPTH_ATTACHMENT, false,
				GL46C.GL_FLOAT),
		DEPTH24_STENCIL8(GL46C.GL_DEPTH24_STENCIL8, GL46C.GL_DEPTH_STENCIL, GL46C.GL_DEPTH_STENCIL_ATTACHMENT, false,
				GL46C.GL_UNSIGNED_INT_24_8),
		DEPTH32_STENCIL8(GL46C.GL_DEPTH32F_STENCIL8, GL46C.GL_DEPTH_STENCIL, GL46C.GL_DEPTH_STENCIL_ATTACHMENT, false,
				GL46C.GL_FLOAT_32_UNSIGNED_INT_24_8_REV);

		public final int internalFormat;
		public final int format;
		public final int attachment;
		public final boolean isColorAttachment;
		public final int dataType;

		private AttachmentFormat(int internalFormat, int format, int attachment, boolean isColorAttachment,
				int dataType) {
			this.internalFormat = internalFormat;
			this.format = format;
			this.attachment = attachment;
			this.isColorAttachment = isColorAttachment;
			this.dataType = dataType;
		}
	}

	public static class FBOAttachment implements OpenGLSurface{
		int ID;
		boolean is_texture;// it is a renderbuffer otherwise
		boolean is_multisampled;
		AttachmentFormat format;
		String name;
		int colorAttachmentNumber;// enum
		int target;
		int width;
		int height;

		FBOAttachment(int ID, boolean isTexture, boolean is_multisampled, 
		AttachmentFormat format, String name,
				int colorAttachmentNumber, int target, int width, int height) {
			this.ID = ID;
			this.is_texture = isTexture;
			this.is_multisampled = is_multisampled;
			this.format = format;
			this.name = name;
			this.colorAttachmentNumber = colorAttachmentNumber;
			this.target = target;
			this.width = width;
			this.height = height;
		}

		public void bind() {
			GL46C.glBindTexture(target, ID);
		}

		public void unbind() {
			GL46C.glBindTexture(target, 0);
		}

		@Override
		public void bindAsTexture(int textureUnit) {
			if (!is_texture) {
				throw new IllegalArgumentException("Error, the FBOAttachment " + name + " isn't a texture");
			}
			GL46C.glActiveTexture(GL46C.GL_TEXTURE0 + textureUnit);
			GL46C.glBindTexture(target, ID);
		}

		public void bindAsTexture(int textureUnit, int filter, int border) {
			bindAsTexture(textureUnit);

			if (is_multisampled) {
				throw new IllegalArgumentException("Filtering is not available for multisampled textures");
			}

			GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MAG_FILTER, filter);
			GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MIN_FILTER, filter);
			GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_WRAP_S, border);
			GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_WRAP_T, border);
		}

		@Override
		public void unbindAsTexture(int textureUnit) {
			if (!is_texture) {
				throw new IllegalArgumentException("Error, the FBOAttachment " + name + " isn't a texture");
			}
			GL46C.glActiveTexture(GL46C.GL_TEXTURE0 + textureUnit);
			GL46C.glBindTexture(target, 0);
		}

		public int getID() {
			return ID;
		}

		public boolean isTexture() {
			return is_texture;
		}

		public boolean isMultisampled() {
			return is_multisampled;
		}

		public AttachmentFormat getFormat() {
			return format;
		}

		public String getName() {
			return name;
		}

		public int getColorAttachmentNumber() {// enum
			return colorAttachmentNumber;
		}

		public int getTarget() {
			return target;
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}

	}

    int ID;
    public int width;
    public int height;
    int MULTISAMPLE_COUNT;
    String name;

    int colorAttachments = 0;
    Map<String, FBOAttachment> attachments;
    List<Integer> drawBuffers; // the color attachments which are currently bound

	/*
	 * Creates a new fbo with a size of 1x1.
	 *
	 * @return a new fbo.
	 */
	public FBO(String name, int MULTISAMPLE_COUNT) {
		this(name, 1, 1, MULTISAMPLE_COUNT);
	}

	/*
	 * Creates a new fbo with the specified dimensions.
	 *
	 * @param width
	 * 
	 * @param height
	 * 
	 * @return a new FBO.
    */
	public FBO(String name, int width, int height, int MULTISAMPLE_COUNT) {
		this.ID=0;
        this.width=width; 
        this.height=height;
        this.MULTISAMPLE_COUNT=MULTISAMPLE_COUNT;
        this.name=name;
        this.attachments = new HashMap<>();
        this.drawBuffers = new ArrayList<>();

        try ( MemoryStack stack = MemoryStack.stackPush() ) {
			IntBuffer pID = stack.mallocInt(1); // int*
    		GL46C.glGenFramebuffers(pID);
			ID = pID.get(0);
		}

	}

	public void delete() {
		for (FBOAttachment attachment : attachments.values()) {
			int id = attachment.getID();
			if (attachment.isTexture()) {
				GL46C.glDeleteTextures(id);
			} else {
				GL46C.glDeleteRenderbuffers(id);
			}
		}

		GL46C.glDeleteFramebuffers(ID);
	}


    /**
    * Make sure to call this method after binding the FBO, even with no
    * parameter. Binds an array of color attachments. An attachment that
    * doesn't exists is ignored.
    *
    * @param colorAttachments
    *            The color attachments that have to be bound.
    *
    */
    public void bindColorAttachments(
            List<String> colorAttachments) {
        drawBuffers.clear();

        for (var attachmentName : colorAttachments) {
            FBOAttachment attachment = getAttachment(attachmentName);
            if (attachment.getFormat().isColorAttachment) {
                drawBuffers.add(attachment.getColorAttachmentNumber());
            } else {
                throw new IllegalArgumentException("Error, " + attachmentName + " isn't a color attachment");
            }
        }

        if (drawBuffers.isEmpty()) {
            drawBuffers.add(GL46C.GL_NONE);
        }

        GL46C.glDrawBuffers(drawBuffers.stream().mapToInt(i->i).toArray());

    }

    /**
    * Sets the read and draw buffers to GL_NONE, ie: there is no color data.
    * Useful for shadow mapping for eg.
    */
    public void bindNoColorBuffers() {
        GL46C.glReadBuffer(GL46C.GL_NONE);
        GL46C.glDrawBuffer(GL46C.GL_NONE);
    }

    /**
    * Sets the viewport to the current size of that FBO.
    */
    public void setViewport() {
        GL46C.glViewport(0, 0, width, height);
    }

    /**
    * Resize all the FBOAttachments of this FBO. Does nothing if the size is
    * the same.
    *
    * @param width
    * @param height
    */
    public void resize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }

        this.width = width;
        this.height = height;

        bind();

        for (FBOAttachment attachment : attachments.values()) {
			attachment.width = width;
			attachment.height = height;

            if (attachment.isTexture()) {

                GL46C.glBindTexture(attachment.getTarget(), attachment.getID());

                if (MULTISAMPLE_COUNT == 0) {
                    GL46C.glTexImage2D(attachment.getTarget(), 0, attachment.getFormat().internalFormat,
                            width, height, 0, attachment.getFormat().format,
                            GL46C.GL_FLOAT,
                            MemoryUtil.NULL);
                } else {
                    GL46C.glTexImage2DMultisample(attachment.getTarget(), MULTISAMPLE_COUNT,
                            attachment.getFormat().internalFormat, width, height,
                            true);
                }

                GL46C.glBindTexture(attachment.getTarget(), 0);
            } else {
                GL46C.glBindRenderbuffer(GL46C.GL_RENDERBUFFER, attachment.getID());

                GL46C.glRenderbufferStorage(GL46C.GL_RENDERBUFFER,
                        attachment.getFormat().format, width, height);

                GL46C.glBindRenderbuffer(GL46C.GL_RENDERBUFFER, 0);
            }
        }

        unbind();
    }

    /**
    * Creates a new texture attachment for this fbo.
    *
    * @param name
    * @param format
    */
    public void addTextureAttachment(String name, AttachmentFormat format) {
        bind();

        int target =
                MULTISAMPLE_COUNT == 0 ? GL46C.GL_TEXTURE_2D : GL46C.GL_TEXTURE_2D_MULTISAMPLE;

        int textureID = GL46C.glCreateTextures(GL46C.GL_TEXTURE_2D);
        // GL46C.glGenTextures(1, &textureID);

        GL46C.glBindTexture(target, textureID);

        if (MULTISAMPLE_COUNT == 0) {
            GL46C.glTexImage2D(target, 0, format.internalFormat, width, height, 0,
                    format.format, format.dataType,
                    (ByteBuffer)null);

            GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_NEAREST);
            GL46C.glTexParameteri(target, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_NEAREST);
        } else {
            GL46C.glTexImage2DMultisample(target, MULTISAMPLE_COUNT,
                    format.internalFormat, width, height, true);
        }

        int colorAttachmentNumber = -1;
        if (format.isColorAttachment) {
            colorAttachmentNumber = this.colorAttachments + format.attachment;

            GL46C.glFramebufferTexture2D(GL46C.GL_FRAMEBUFFER, colorAttachmentNumber, target,
                    textureID, 0);

            this.colorAttachments++;
        } else {
            GL46C.glFramebufferTexture2D(GL46C.GL_FRAMEBUFFER, format.attachment, target,
                    textureID, 0);
        }

        FBOAttachment attachment = new FBOAttachment(textureID, true, MULTISAMPLE_COUNT != 0, 
			format, name, colorAttachmentNumber, target, width, height);

        this.attachments.put(name, attachment);

        GL46C.glBindTexture(target, 0);
        unbind();

    }

    
	public FBOAttachment getAttachment(String attachmentName) {
		FBOAttachment it = attachments.get(attachmentName);
		if (it == null) {
			String allAttachmentNames = "";
			for(String name : attachments.keySet()){
				allAttachmentNames += name + " ";
			}
			throw new IllegalArgumentException("Error, unknown FBO attachment name: "
					+ attachmentName + " for fbo " + name + ", all attachments: " + allAttachmentNames);
		} else {
			return it;
		}
	}

    /**
    * Binds this framebuffer. Make sure to unbind the fbo when you are finished
    * using it ! This method is called automatically when attachments are
    * created.
    */
    public void bind() {
        //Bind both to read and draw framebuffers
        GL46C.glBindFramebuffer(GL46C.GL_FRAMEBUFFER, ID);
    }

    /**
    * Unbinds the current FBO.
    */
    public void unbind() {
        GL46C.glBindFramebuffer(GL46C.GL_FRAMEBUFFER, 0);
    }


    /**
    * @return true if the FBO is complete.
    */
    public boolean finish() {
        boolean success = false;

        bind();
        drawBuffers.clear();
        drawBuffers.add(GL46C.GL_NONE);
        GL46C.glDrawBuffers(drawBuffers.stream().mapToInt(i->i).toArray());

        if (GL46C.glCheckFramebufferStatus(GL46C.GL_FRAMEBUFFER) == GL46C.GL_FRAMEBUFFER_COMPLETE) {
            success = true;
        }

        unbind();
        return success;
    }

    /**
    * Clears a specific color attachment. The attachment must have been bound
    * with <code>bindColorAttachments(String... colorAttachments)</code>
    * otherwise an exception is thrown
    *
    * @param attachmentName
    *            The name of the color attachment.
    * @param color
    *            The color to clear the attachment with.
    */
    public void clearColorAttachment(String attachmentName, Vector4f color) {

        FBOAttachment attachment = getAttachment(attachmentName);
        if (attachment.getFormat().isColorAttachment) {

            for (int i = 0; i < drawBuffers.size(); i++) {
                if (attachment.getColorAttachmentNumber() == drawBuffers.get(i)) {
                    float[] buffer = new float[] { color.x, color.y, color.z, color.w };
                    GL46C.glClearBufferfv(GL46C.GL_COLOR, i, buffer);
                    return;
                }
            }

            throw new IllegalArgumentException("FBO color attachment " + attachmentName
                    + " isn't currently bound, cannot clear");
        }
        throw new IllegalArgumentException(attachmentName + " isn't a color buffer");
    }

    /**
    * Clears the depth buffer attachment of that FBO. Make sure depth writing
    * is enabled, otherwise nothing will happen.
    *
    * @param value
    *            The float value to clear the buffer with. Should be in the range
    *            [0.0f, 1.0f]
    */
    public void clearDepthAttachment(float value) {
        float v[] = new float[]{value};
        GL46C.glClearBufferfv(GL46C.GL_DEPTH, 0, v);
    }

    /**
    * Clears the stencil buffer attachment of that FBO.
    *
    * @param value
    *            The int value to clear the buffer with. Must be in the range
    *            [0, 2^m-1] where m is the number of bits in the stencil
    *            buffer.
    */
    public void clearStencilAttachment(int value) {
        int v[] = new int[]{value};
        GL46C.glClearBufferiv(GL46C.GL_STENCIL, 0, v);
    }

}
