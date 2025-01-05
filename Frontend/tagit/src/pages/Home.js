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

    const playerId = Math.random().toString(36).substr(2, 9);
    const payload = {
      playerId: playerId,
      playerName: username.trim()
    };

    try {
      console.log('Sending payload:', payload);
      const response = await axios.post('http://localhost:8080/api/rooms/join', payload);
      console.log('Response from server:', response.data);
      
      // Store in sessionStorage instead of localStorage
      sessionStorage.setItem('playerName', username.trim());
      sessionStorage.setItem('playerId', playerId);

      navigate(`/room/${response.data.roomId}`);
    } catch (error) {
      console.error('Full error details:', error.response || error);
      setError('Failed to join room. Please try again.');
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