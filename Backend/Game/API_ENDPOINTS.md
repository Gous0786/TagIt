# API Endpoints Documentation

## Room Management

### Join Room
- **POST** `/api/rooms/join`
- **Body**: Player object
- **Response**: Room object
- **Description**: Assigns a player to an available room or creates a new one

### Get Active Rooms
- **GET** `/api/rooms?page={page}&size={size}`
- **Parameters**:
  - `page` (optional, default: 0): Page number
  - `size` (optional, default: 20): Items per page
- **Response**: List of Room objects
- **Description**: Returns paginated list of active game rooms

### Leave Room
- **POST** `/api/rooms/leave`
- **Parameters**:
  - `playerId`: ID of the player leaving
- **Response**: Empty response (200 OK)
- **Description**: Removes a player from their current room

### Get Room Players
- **GET** `/api/rooms/{roomId}/players`
- **Parameters**:
  - `roomId`: Room identifier
- **Response**: List of Player objects
- **Description**: Returns all players in a specific room

### Set Player Ready Status
- **POST** `/api/rooms/ready`
- **Parameters**:
  - `playerId`: Player identifier
  - `ready`: Boolean ready status
- **Response**: Empty response (200 OK)
- **Description**: Updates player's ready status in the room

### Reset Room
- **POST** `/api/rooms/{roomId}/reset`
- **Parameters**:
  - `roomId`: Room identifier
- **Response**: Updated Room object
- **Description**: Resets the room to its initial state

### Get Room Time
- **GET** `/api/rooms/{roomId}/time`
- **Parameters**:
  - `roomId`: Room identifier
  - `phase`: Game phase name
- **Response**: Map with `timeLeft` value
- **Description**: Returns remaining time for the specified game phase

### List All Rooms
- **GET** `/api/rooms/list`
- **Response**: List of RoomInfo objects
- **Description**: Returns simplified information about all rooms

### Get Room Image
- **GET** `/api/rooms/{roomId}/image`
- **Parameters**:
  - `roomId`: Room identifier
- **Response**: Map with `imageUrl`
- **Description**: Returns the generated image URL for the room

### Generate Room Image
- **POST** `/api/rooms/{roomId}/generate-image`
- **Parameters**:
  - `roomId`: Room identifier
  - `customPrompt` (optional): Custom image generation prompt
- **Response**: Map with `imageUrl` or error message
- **Description**: Forces generation/regeneration of room image

## Comments System

### Add Comment
- **POST** `/api/comments/room/{roomId}`
- **Parameters**:
  - `roomId`: Room identifier
  - `playerId`: Player identifier
  - `playerName`: Player's display name
- **Body**: Map with `content` key
- **Response**: Created Comment object
- **Description**: Adds a new comment to the room

### Get Room Comments
- **GET** `/api/comments/room/{roomId}`
- **Parameters**:
  - `roomId`: Room identifier
- **Response**: List of Comment objects
- **Description**: Returns all comments for a specific room

### Like Comment
- **POST** `/api/comments/{commentId}/like`
- **Parameters**:
  - `commentId`: Comment identifier
  - `playerId`: Player identifier
- **Response**: Updated Comment object
- **Description**: Toggles like status for a comment

### Get Comment Leaderboard
- **GET** `/api/comments/room/{roomId}/leaderboard`
- **Parameters**:
  - `roomId`: Room identifier
  - `playerId`: Player identifier
- **Response**: Map with leaderboard data
- **Description**: Returns comment engagement leaderboard for the room

### Get Top Comments
- **GET** `/api/comments/room/{roomId}/top`
- **Parameters**:
  - `roomId`: Room identifier
  - `limit` (optional, default: 3): Number of top comments to return
- **Response**: List of top Comment objects
- **Description**: Returns the most-liked comments in the room

### Batch Like Comments
- **POST** `/api/comments/batch-like`
- **Parameters**:
  - `playerId`: Player identifier
- **Body**: List of comment IDs
- **Response**: List of updated Comment objects
- **Description**: Processes multiple comment likes in a single request

## Debug/Development Endpoints

### Get All Rooms (Debug)
- **GET** `/api/rooms/debug/all`
- **Parameters**:
  - `page` (optional, default: 0): Page number
  - `size` (optional, default: 20): Items per page
- **Response**: List of all Room objects
- **Description**: Development endpoint to view all rooms regardless of status

### Clear All Rooms (Debug)
- **DELETE** `/api/rooms/debug/clear-all`
- **Response**: Empty response (200 OK)
- **Description**: Development endpoint to remove all rooms

## WebSocket Endpoints

### Room Updates
- **Subscribe to**: `/topic/room/{roomId}`
- **Description**: Receives real-time updates for room state changes 