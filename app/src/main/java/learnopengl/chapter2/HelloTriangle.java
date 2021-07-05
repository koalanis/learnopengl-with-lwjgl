package learnopengl.chapter2;

import learnopengl.Runner;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class HelloTriangle implements Runner {
    // The window handle
    private long window;

    private int windowWidth = 800;
    private int windowHeight = 600;

    final int BYTES_PER_FLOAT = 4;

    final int VERTEX_POS_SIZE   = 3; // x, y

    final int VERTEX_STRIDE =  ( BYTES_PER_FLOAT * VERTEX_POS_SIZE );

    public static String tinyVertShader = "" +
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);\n" +
            "}";


    public static String tinyFragShader = "" +
            "#version 330 core\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);\n" +
            "} ";


    private int windowHandle;

    private int vbo;
    private int vao;
    private int ebo;

    private float[] vertices;
    private int[] indices;
    private int shaderProgram;


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

        int vertShaderHandle = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertShaderHandle, tinyVertShader);
        glCompileShader(vertShaderHandle);
        checkIfShaderCompiledSuccessfully(vertShaderHandle);

        int fragShaderHandle = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragShaderHandle, tinyFragShader);
        glCompileShader(fragShaderHandle);
        checkIfShaderCompiledSuccessfully(fragShaderHandle);

        this.shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertShaderHandle);
        glAttachShader(shaderProgram, fragShaderHandle);
        glLinkProgram(shaderProgram);

        checkIfProgramLinkedSuccessfully(shaderProgram);
        glUseProgram(shaderProgram);
        glDeleteShader(vertShaderHandle);
        glDeleteShader(fragShaderHandle);

    }

    private void createVertexData() {

        vertices = new float[]{
                0.5f, 0.5f, 0.0f,  // top right
                0.5f, -0.5f, 0.0f,  // bottom right
                -0.5f, -0.5f, 0.0f,  // bottom left
                -0.5f, 0.5f, 0.0f   // top left
        };

        indices = new int[]{
                0, 1, 3,  // first Triangle
                1, 2, 3   // second Triangle
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, VERTEX_POS_SIZE, GL_FLOAT,false, VERTEX_STRIDE, 0);
        glEnableVertexAttribArray(0);

        // note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // remember: do NOT unbind the EBO while a VAO is active as the bound element buffer object IS stored in the VAO; keep the EBO bound.
        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        // You can unbind the VAO afterwards so other VAO calls won't accidentally modify this VAO, but this rarely happens. Modifying other
        // VAOs requires a call to glBindVertexArray anyways so we generally don't unbind VAOs (nor VBOs) when it's not directly necessary.
        glBindVertexArray(0);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);

        createShaders();
        createVertexData();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {

            // render
            // ------
            glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

            glUseProgram(shaderProgram);
            glBindVertexArray(vao);
            //glDrawArrays(GL_TRIANGLES, 0, 6);
            glDrawElements(GL_TRIANGLES, indices.length,GL_UNSIGNED_INT, 0);
            glBindVertexArray(0); // no need to unbind it every time


            glfwSwapBuffers(window); // swap the color buffers
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void checkIfProgramLinkedSuccessfully(int shaderProgram) {
        if(glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_TRUE) {
            System.out.println("program compiled successfully");
        } else {
            System.out.printf("glGetProgramInfoLog :: %s%n", glGetProgramInfoLog(shaderProgram));
            throw new RuntimeException("Shader Program failed linking");
        }
    }

    private void checkIfShaderCompiledSuccessfully(int vertShaderHandle) {
        if(glGetShaderi(vertShaderHandle, GL_COMPILE_STATUS) == GL_TRUE) {
            System.out.println("shader compiled successfully");
        } else {
            System.out.printf("glGetShaderInfoLog :: %s%n", glGetShaderInfoLog(vertShaderHandle));
            throw new RuntimeException("Shader failed compilation");
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
        new HelloTriangle().run();
    }
}
