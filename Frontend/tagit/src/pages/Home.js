import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';
import axios from 'axios';

function Home() {
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleJoinGame = async () => {
    if (!username.trim()) {
      setError('Please enter a username');
      return;
    }

    try {
      const playerId = Math.random().toString(36).substr(2, 9);
      
      // Store in sessionStorage
      sessionStorage.setItem('playerName', username.trim());
      sessionStorage.setItem('playerId', playerId);
      
      console.log('Stored player info:', {
        playerId,
        playerName: username.trim()
      });

      const response = await axios.post('http://localhost:8080/api/rooms/join', {
        playerId: playerId,
        playerName: username.trim()
      });

      navigate(`/room/${response.data.roomId}`);
    } catch (error) {
      console.error('Error joining game:', error);
      setError('Failed to join game');
    }
  };

  return (
    <div className="instagram-container">
      <div className="bubble">👍</div>
      <div className="bubble">❤️</div>
      <div className="bubble">🔥</div>
      <div className="bubble">🎉</div>
      <div className="instagram-header">
        <h2>Welcome to the Game</h2>
      </div>
      <div className="game-title">
        <h1>TagIt</h1>
        <p className="tagline">Create, Share, Tag Your Memes!</p>
      </div>
      
      <div className="content">
        <p>Join a room to start playing!</p>
        <div className="join-form">
          <input
            type="text"
            placeholder="Enter your username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <button onClick={handleJoinGame}>Join Game</button>
        </div>
        {error && <div className="error-message">{error}</div>}
      </div>
    </div>
  );
}

export default Home; 