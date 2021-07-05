package learnopengl.chapter6;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import learnopengl.Runner;
import learnopengl.chapter5.Transformations;
import learnopengl.utils.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class CoordinateSystems implements Runner {

    // The window handle
    private long window;

    private int windowWidth = 800;
    private int windowHeight = 600;

    final int BYTES_PER_FLOAT = 4;

    final int VERTEX_POS_SIZE   = 3; // x, y
    final int VERTEX_COLOR_SIZE = 4; // r, g, b, and a
    final int VERTEX_UV_SIZE = 2; // u,v

    final int VERTEX_STRIDE =  ( BYTES_PER_FLOAT * (VERTEX_POS_SIZE + VERTEX_COLOR_SIZE + VERTEX_UV_SIZE));

    private int vbo;
    private int vao;
    private int ebo;

    private float[] vertices;

    private Shader shaderProgram;
    private int texture1;
    private int texture2;
    private ArrayList<Vector3f> cubePositions;

    public void run() {
        init();
        loop();
        destroy();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(windowWidth, windowHeight, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void createShaders() {
        InputStream isVs = getClass().getClassLoader().getResourceAsStream("shaders/coordinatesystems/coordinatesystems.vs.glsl");
        InputStream isFs = getClass().getClassLoader().getResourceAsStream("shaders/coordinatesystems/coordinatesystems.fs.glsl");
        String vs, fs;
        try {
            vs = CharStreams.toString(new InputStreamReader(isVs, Charsets.US_ASCII));
            fs = CharStreams.toString(new InputStreamReader(isFs, Charsets.US_ASCII));
        } catch (IOException e) {
            vs = null;
            fs = null;
        }

        this.shaderProgram = Shader.createShader(vs, null, fs);
    }

    private void createVertexData() {

        vertices = new float[]  {
                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,

                -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,

                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,

                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,

                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,

                -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f, 1.0f,   0.0f, 1.0f
        };


        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        {
            glBindVertexArray(vao);

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

            // position attribute
            glVertexAttribPointer(0, VERTEX_POS_SIZE, GL_FLOAT, false, VERTEX_STRIDE, 0);
            glEnableVertexAttribArray(0);

            // color attribute
            glVertexAttribPointer(1, VERTEX_COLOR_SIZE, GL_FLOAT, false, VERTEX_STRIDE, VERTEX_POS_SIZE * BYTES_PER_FLOAT);
            glEnableVertexAttribArray(1);

            // texture coord attribute
            glVertexAttribPointer(2, VERTEX_UV_SIZE, GL_FLOAT, false, VERTEX_STRIDE, (VERTEX_POS_SIZE + VERTEX_COLOR_SIZE) * BYTES_PER_FLOAT);
            glEnableVertexAttribArray(2);

            // note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glBindVertexArray(0);

            this.cubePositions = new ArrayList<>();
            cubePositions.add(new Vector3f( 0.0f,  0.0f,  0.0f));
            cubePositions.add(new Vector3f( 2.0f,  5.0f, -15.0f));
            cubePositions.add(new Vector3f(-1.5f, -2.2f, -2.5f));
            cubePositions.add(new Vector3f(-3.8f, -2.0f, -12.3f));
            cubePositions.add(new Vector3f( 2.4f, -0.4f, -3.5f));
            cubePositions.add(new Vector3f(-1.7f,  3.0f, -7.5f));
            cubePositions.add(new Vector3f( 1.3f, -2.0f, -2.5f));
            cubePositions.add(new Vector3f( 1.5f,  2.0f, -2.5f));
            cubePositions.add(new Vector3f( 1.5f,  0.2f, -1.5f));
            cubePositions.add(new Vector3f(-1.3f,  1.0f, -1.5f));
        }
    }

    private void createTextures() {
        this.texture1 = glGenTextures();
        {
            glBindTexture(GL_TEXTURE_2D, texture1);
            // set the texture wrapping parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);    // set texture wrapping to GL_REPEAT (default wrapping method)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            // set texture filtering parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            STBImage.stbi_set_flip_vertically_on_load(true);
            IntBuffer w1 = BufferUtils.createIntBuffer(1);
            IntBuffer h1 = BufferUtils.createIntBuffer(1);
            IntBuffer d1 = BufferUtils.createIntBuffer(1);
            String path = null;
            try {
                path = Paths.get(getClass().getClassLoader().getResource("images/container.jpg").toURI()).toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            ByteBuffer image1 = STBImage.stbi_load(path, w1, h1, d1, 0);
            if (Objects.nonNull(image1) && image1.capacity() > 0) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w1.get(), h1.get(), 0, GL_RGB, GL_UNSIGNED_BYTE, image1);
                glGenerateMipmap(GL_TEXTURE_2D);
            } else {
                System.out.println("Failed to load texture");
            }
            STBImage.stbi_image_free(image1);
        }

        this.texture2 = glGenTextures();
        {
            glBindTexture(GL_TEXTURE_2D, texture2);
            // set the texture wrapping parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);    // set texture wrapping to GL_REPEAT (default wrapping method)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            // set texture filtering parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            IntBuffer w2 = BufferUtils.createIntBuffer(1);
            IntBuffer h2 = BufferUtils.createIntBuffer(1);
            IntBuffer d2 = BufferUtils.createIntBuffer(1);
            String path2 = null;
            try {
                path2 = Paths.get(getClass().getClassLoader().getResource("images/awesomeface.png").toURI()).toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            ByteBuffer image2 = STBImage.stbi_load(path2, w2, h2, d2, 0);
            if (Objects.nonNull(image2) && image2.capacity() > 0) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w2.get(), h2.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, image2);
                glGenerateMipmap(GL_TEXTURE_2D);
            } else {
                System.out.println("Failed to load texture");
            }

            STBImage.stbi_image_free(image2);
        }

        glUseProgram(shaderProgram.getHandle());
        glUniform1i(glGetUniformLocation(shaderProgram.getHandle(), "texture1"), 0);
        glUniform1i(glGetUniformLocation(shaderProgram.getHandle(), "texture2"), 1);

    }

    private void loop() {
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        createShaders();
        createVertexData();
        createTextures();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {

            // render
            // ------
            glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // also clear the depth buffer now!

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture1);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, texture2);

            glUseProgram(shaderProgram.getHandle());
            glBindVertexArray(vao);

            float radius = 10.0f;
            float camX = (float) Math.sin(1.0f*glfwGetTime()) * radius;
            float camZ = (float) Math.cos(1.0f*glfwGetTime()) * radius;

            FloatBuffer view = BufferUtils.createFloatBuffer(16);
            int viewLoc = glGetUniformLocation(shaderProgram.getHandle(), "view");
            new Matrix4f()
                    .lookAt(new Vector3f(camX, 0.0f, camZ), new Vector3f(0.0f), new Vector3f(0.0f, 1.0f, 0.0f))
                    .get(view);


            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            int projectionLoc = glGetUniformLocation(shaderProgram.getHandle(), "projection");
            new Matrix4f().identity()
                    .perspective((float) Math.toRadians(45.0), (float)windowWidth/(float)windowHeight, 0.1f, 100.0f)
                    .get(projection);

            glUniformMatrix4fv(viewLoc, false, view);
            glUniformMatrix4fv(projectionLoc, false, projection);

            int i = 0;
            for (Vector3f m : cubePositions) {
                // calculate the model matrix for each object and pass it to shader before drawing

                FloatBuffer model = BufferUtils.createFloatBuffer(16);
                int modelLoc = glGetUniformLocation(shaderProgram.getHandle(), "model");
                float angle = 20.0f * i++;

                new Matrix4f().identity().translate(m).rotate((float) Math.toRadians(angle), 1.0f, .3f, .5f).get(model);
                glUniformMatrix4fv(modelLoc, false, model);
                glDrawArrays(GL_TRIANGLES, 0, 36);
            }

            glBindVertexArray(0); // no need to unbind it every time

            glfwSwapBuffers(window); // swap the color buffers
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void destroy() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) {
        new CoordinateSystems().run();
    }


}
