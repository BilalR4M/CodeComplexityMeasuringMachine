# Code Complexity Measuring Machine

## Overview

The Code Complexity Measuring Machine (CCMM) is a comprehensive tool designed to analyze and measure the complexity of codebases. It provides various metrics such as Halstead metrics, Maintainability Index, and Line Counts to help developers understand and optimize their code.

## Features

- **Halstead Metrics Calculation**: Measures vocabulary, length, volume, difficulty, and effort of the code.
- **Maintainability Index Calculation**: Computes the maintainability index based on cyclomatic complexity, lines of code, and Halstead volume.
- **Line Count Analysis**: Counts the number of code lines, comment lines, blank lines, and unique methods.
- **Real-time Collaboration**: Supports real-time collaboration with features like file editing, saving, and downloading.
- **Integrated Group Chat**: Allows team members to communicate seamlessly within the platform.

## Project Structure

CCMM/
├── client/
│ ├── .eslintrc.cjs
│ ├── .gitignore
│ ├── .prettierrc.json
│ ├── index.html
│ ├── package.json
│ ├── postcss.config.cts
│ ├── public/
│ │ ├── favicon/
│ │ ├── images/
│ ├── src/
│ │ ├── api/
│ │ ├── App.tsx
│ │ ├── assets/
│ │ ├── components/
│ │ ├── context/
│ │ ├── hooks/
│ │ ├── pages/
│ │ ├── styles/
│ │ ├── types/
│ │ ├── utils/
│ ├── tailwind.config.ts
│ ├── tsconfig.json
│ ├── tsconfig.node.json
│ ├── vercel.json
│ ├── vite.config.mts
│
├── server/
│ ├── .gitignore
│ ├── package.json
│ ├── public/
│ ├── src/
│ │ ├── server.ts
│ | ├── types/
│ ├── tsconfig.json
│
├── CCMM-Metrics-API/
│ ├── .gitignore
│ ├── mvnw
│ ├── mvnw.cmd
│ ├── pom.xml
│ ├── src/
│ │ ├── main/
│ │ ├── test/
│ ├── target/
│ | ├── classes/
│ | ├── test-classes/
├── README.md
├── sampleCode.txt

## Installation

### Client

1. Navigate to the `client` directory:

   ```sh
   cd CCMM/client
   ```

2. Install the dependencies:

   ```sh
   npm install
   ```

3. Start the development server:
   ```sh
   npm run dev
   ```

### Server

1. Navigate to the server directory:

   ```sh
   cd CCMM/server
   ```

2. Install the dependencies:

   ```sh
   npm install
   ```

3. Start the server:
   ```sh
   npm run dev
   ```

### Metrics API

1. Navigate to the CCMM-Metrics-API directory:

   ```sh
   cd CCMM-Metrics-API
   ```

2. Build the project using Maven:

   ```sh
   ./mvnw clean install
   ```

3. Run the Spring Boot application:
   ```sh
   ./mvnw spring-boot:run
   ```

## Usage

1. Open the client application in your browser.
2. Upload or create new files to analyze.
3. Use the provided tools to calculate Halstead metrics, Maintainability Index, and Line Counts.
4. Collaborate with team members in real-time and use the integrated chat for communication.
