# Ray Tracing in Java and OpenGL

This repository contains a **GPU-based ray tracing engine** implemented in **Java** and **OpenGL**, developed by 
following the [Ray Tracing in One Weekend book series](https://raytracing.github.io/). The project demonstrates the practical implementation of
real-time ray tracing techniques with GPU acceleration. Due to inherent design differences between `GLSL` and `C++`,
certain techniques are implemented differently while achieving the same results.

![book 2 final scene](./galleries/book2_final.jpg)

## Features
- **GPU Acceleration**: Leverages the power of modern GPUs for high-performance ray tracing.
- **Java and OpenGL**: Built using Java as the primary programming language and OpenGL for rendering.
- **Real-Time Rendering**: Observe the rendering process in real-time, from initial ray casting to the final image composition.

## Requirements
- **Java JDK**: Version 17 or later.
- **OpenGL**: A system with OpenGL 4.0+ support.
- **Windows Operating System**: Only Windows natives (OpenGL & ImGUI) are included in the dependencies, but by 
configuring that in [build.gradle](./build.gradle), you should also be able to run the program on other OS.
- A compatible GPU with sufficient computational power.

## Getting Started
1. Clone the repository:
   ```bash  
   git clone https://github.com/Bowen951209/raytracing-book.git  
   cd raytracing-book  
   ```
2. Build the project using Gradle or your favorite IDE:

This is how to build and run with Gradle:
```bash
  ./gradlew run --args="scene_id"
```
Replace `scene_id` with the desired scene ID.

## Progress
The features all the way to book 2 are implemented.

## License
This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for details.  

## Acknowledgments
This project is based on the concepts and tutorials from the [Ray Tracing in One Weekend book series](https://raytracing.github.io/). Special
thanks to the open-source community for providing tools and resources that made this implementation possible.