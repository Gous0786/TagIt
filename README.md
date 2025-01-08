# TagIt
Meme battle Royale

# TagIt - Meme Battle Royale

A real-time multiplayer game where players compete to create and vote on the funniest memes. Join a room, get a random image, add your creative text, and let the meme battles begin!

## Features

- Real-time multiplayer meme creation and voting
- Drag & drop meme text editor with classic Impact font
- Live voting system
- Leaderboards and winner showcase
- WebSocket-based real-time updates
- AWS integration for image storage and generation

## Tech Stack

### Frontend
- React.js
- Material UI
- Fabric.js for meme editor
- SockJS/STOMP for WebSocket communication
- AWS S3 for image storage

### Backend  
- Spring Boot
- WebSocket/STOMP
- AWS DynamoDB
- AWS Bedrock for image generation
- JWT Authentication

## Installation

### Prerequisites
- Node.js 16+
- Java 17
- Maven
- AWS Account with S3, DynamoDB and Bedrock access

### Backend Setup
1. Clone the repository
2. Run `mvn clean install` to build the project
3. Run `mvn spring-boot:run` to start the server

### Frontend Setup
1. Clone the repository
2. Run `npm install` to install dependencies
3. Run `npm start` to start the frontend
  make sure to setup the AWS credentials and services before running the application
