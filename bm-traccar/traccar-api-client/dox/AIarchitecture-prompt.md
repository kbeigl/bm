# Architecture Documentation & Maintenance Prompt for ILoveDotNet

As an software architect familiar with Spring and Apache Software, assist me with 
analyzing and documenting this codebase architecture using the following approaches:

## INSTRUCTIONS
Analyze this codebase to create visual representations of the architecture that reflects the current state of the project.

## Task 1: Building Architecture Diagrams from Code

Please perform the following tasks in sequence:

1. **Analyze Application Structure**
   - Examine the entire traccar-api-client structure including all projects
   - Analyze how REST and Websocket libraries are organized and referenced
   - Identify key services and their dependencies

2. **Create Visual Representation**
   - Show the structure visually using Mermaid syntax
   - Create these three specific diagrams:
     1. Project structure diagram - showing how projects relate to each other
     2. Component interaction diagram - showing the runtime flow 
     3. Deployment architecture diagram
   - Ensure all diagrams use valid Mermaid syntax with appropriate styling
   - Show clear relationships between components with proper node labels

3. **Document Architecture Visually**
   - Create a new file file at `dox/AIarchitecture.md`
   - Include all Mermaid diagrams from step 2
   - Add explanatory text between diagrams
   - Include sections for Key Architectural Characteristics, Component Types, and Technical Implementation