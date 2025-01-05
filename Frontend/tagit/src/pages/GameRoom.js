import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useGame } from '../context/GameContext';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import './GameRoom.css';
import { 
  Card, 
  CardContent, 
  Typography, 
  IconButton, 
  Box 
} from '@mui/material';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import { Canvas, Text, FabricImage } from 'fabric';
import { uploadToS3, deleteFromS3 } from '../utils/s3Config';

// Game stages
const STAGES = {
  WAITING: 'waiting',
  COMMENTING: 'commenting',
  LIKING: 'liking',
  RESULTS: 'results'
};

// Add this at the top of the file, outside of the component
const playerColors = new Map();

// Add this near the top of your component
const randomImageUrl = "https://source.unsplash.com/random/600x600/?funny";

// Move BubbleBackground outside of GameRoom component and memoize it
const BubbleBackground = React.memo(() => (
  <div className="bubble-background">
    <div className="bubble">❤️</div>
    <div className="bubble">👍</div>
    <div className="bubble">🔥</div>
    <div className="bubble">✨</div>
    <div className="bubble">💫</div>
    <div className="bubble">🌟</div>
  </div>
));

function GameRoom() {
  const { roomId } = useParams();
  const { player } = useGame();
  const navigate = useNavigate();
  const [players, setPlayers] = useState([]);
  const [gameImage, setGameImage] = useState(null);
  const [comment, setComment] = useState('');
  const [timeLeft, setTimeLeft] = useState(60);
  const [gameStage, setGameStage] = useState(STAGES.WAITING);
  const [stompClient, setStompClient] = useState(null);
  const [comments, setComments] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [likedComments, setLikedComments] = useState(new Set());
  const timerIdRef = useRef(null);
  const hasSubmittedRef = useRef(false);
  const [commentingStarted, setCommentingStarted] = useState(false);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [canvas, setCanvas] = useState(null);
  const [memeText, setMemeText] = useState('');
  const [textPosition, setTextPosition] = useState('top'); // 'top' or 'bottom'
  const canvasRef = useRef(null);

  // Add this function to get consistent colors
  const getPlayerColor = (playerId) => {
    if (!playerColors.has(playerId)) {
      playerColors.set(playerId, `hsl(${Math.random() * 360}, 70%, 60%)`);
    }
    return playerColors.get(playerId);
  };

  // Move fetchPlayers outside of useEffect
  const fetchPlayers = async () => {
    try {
      const response = await axios.get(`http://localhost:8080/api/rooms/${roomId}/players`);
      console.log('Initial players fetch:', response.data);
      const validPlayers = response.data.map(p => ({
        id: p.playerId || p.id,
        username: p.playerName || p.playerId,
        ready: p.ready || false
      }));
      setPlayers(validPlayers);
      
      // Only check for game start on initial load or websocket updates
      if (!commentingStarted && validPlayers.length === 4) {
        startCommentingPhase();
        setCommentingStarted(true);
      }
    } catch (error) {
      console.error('Error fetching players:', error);
    }
  };

  useEffect(() => {
    // Set up polling for just the player list
    const fetchPlayers = async () => {
      try {
        const response = await axios.get(`http://localhost:8080/api/rooms/${roomId}/players`);
        console.log('Initial players fetch:', response.data);
        const validPlayers = response.data.map(p => ({
          id: p.playerId || p.id,
          username: p.playerName || p.playerId,
          ready: p.ready || false
        }));
        setPlayers(validPlayers);
        
        // Only check for game start on initial load or websocket updates
        if (!commentingStarted && validPlayers.length === 4) {
          startCommentingPhase();
          setCommentingStarted(true);
        }
      } catch (error) {
        console.error('Error fetching players:', error);
      }
    };

    // Only poll if we're in the waiting stage
    if (gameStage === STAGES.WAITING) {
      fetchPlayers();
      const pollInterval = setInterval(fetchPlayers, 3000);
      return () => clearInterval(pollInterval);
    }
  }, [roomId, gameStage]); // Add gameStage as dependency

  // Move WebSocket setup to a separate useEffect
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => {
        console.log('STOMP:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('Connected to STOMP');
      
      // Subscribe to room updates
      client.subscribe(`/topic/room/${roomId}/players`, (message) => {
        console.log('Received player update:', message.body);
        const data = JSON.parse(message.body);
        const validPlayers = data.map(p => ({
          id: p.playerId || p.id,
          username: p.playerName,
          ready: p.ready || false
        }));
        setPlayers(validPlayers);

        // Check if we should start the game
        if (validPlayers.length === 4 && !commentingStarted) {
          startCommentingPhase();
          setCommentingStarted(true); // Set the flag to true
        }
      });

      client.subscribe(`/topic/room/${roomId}/phase`, (message) => {
        console.log('Received phase update:', message.body);
        const data = JSON.parse(message.body);
        handlePhaseChange(data);
      });

      // Subscribe to timer updates
      client.subscribe('/topic/timer', (message) => {
        const currentTime = JSON.parse(message.body);
        setTimeLeft(currentTime);
      });

      // Then join room
      client.publish({
        destination: `/app/room/${roomId}/join`,
        body: JSON.stringify({
          playerId: player.id,
          username: player.username
        })
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
    };

    client.activate();

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, [roomId, player.id, player.username]);

  const handlePhaseChange = (data) => {
    console.log('Handling phase change:', data);
    switch (data.phase) {
      case 'COMMENTING':
        startCommentingPhase();
        break;
      case 'LIKING':
        startLikingPhase();
        break;
      case 'RESULTS':
        showResults();
        break;
      default:
        console.log('Unknown phase:', data.phase);
    }
  };

  const fetchServerTime = async (phase) => {
    try {
      const response = await axios.get(`http://localhost:8080/api/rooms/${roomId}/time`, {
        params: { phase }
      });
      if (response.data.timeLeft >= 0) {
        setTimeLeft(response.data.timeLeft);
      }
    } catch (error) {
      console.error('Error fetching server time:', error);
    }
  };

  const formatTime = (seconds) => {
    return `${Math.floor(seconds / 60)}:${(seconds % 60).toString().padStart(2, '0')}`;
  };

  const startCommentingPhase = async () => {
    console.log('Starting commenting phase');
    setGameStage(STAGES.COMMENTING);
    setTimeLeft(60);
    await fetchGameImage();

    hasSubmittedRef.current = false;

    if (timerIdRef.current) clearInterval(timerIdRef.current);

    timerIdRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerIdRef.current);
          if (!hasSubmittedRef.current && comment.trim()) {
            submitComment();
          }
          setIsTransitioning(true);
          
          setTimeout(async () => {
            await fetchComments();
            setIsTransitioning(false);
            startLikingPhase();
          }, 3000);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const startLikingPhase = async () => {
    console.log('Starting liking phase');
    setGameStage(STAGES.LIKING);
    setTimeLeft(60);
    
    // Fetch comments before starting the timer
    await fetchComments();
    console.log('Comments after fetch:', comments);

    if (timerIdRef.current) clearInterval(timerIdRef.current);

    timerIdRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerIdRef.current);
          setGameStage(STAGES.RESULTS);
          fetchLeaderboard();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const showResults = async () => {
    setGameStage(STAGES.RESULTS);
    await fetchLeaderboard();
  };

  const fetchGameImage = async () => {
    try {
      const response = await axios.get(`http://localhost:8080/api/rooms/${roomId}/image`);
      setGameImage(response.data.imageUrl);
    } catch (error) {
      console.error('Error fetching image:', error);
    }
  };

  const fetchComments = async () => {
    try {
      const response = await axios.get(`http://localhost:8080/api/comments/room/${roomId}`);
      console.log('Fetched comments:', response.data);
      setComments(response.data);
    } catch (error) {
      console.error('Error fetching comments:', error);
    }
  };

  const fetchLeaderboard = async () => {
    try {
      const storedPlayerId = sessionStorage.getItem('playerId');
      const response = await axios.get(
        `http://localhost:8080/api/comments/room/${roomId}/leaderboard`,
        {
          params: {
            playerId: storedPlayerId
          }
        }
      );
      console.log('Leaderboard data:', response.data);
      setLeaderboard(response.data);
    } catch (error) {
      console.error('Error fetching leaderboard:', error);
    }
  };

  const handleLike = async (commentId) => {
    if (likedComments.has(commentId)) return;

    // Get the stored playerId
    const storedPlayerId = localStorage.getItem('playerId');

    if (!storedPlayerId) {
      console.error('No player ID found');
      return;
    }

    try {
      console.log('Liking comment:', {
        commentId,
        playerId: storedPlayerId
      });

      await axios.post(
        `http://localhost:8080/api/comments/${commentId}/like`,
        null,  // no request body needed
        {
          params: {
            playerId: storedPlayerId
          }
        }
      );

      setLikedComments(prev => new Set(prev).add(commentId));
    } catch (error) {
      console.error('Error liking comment:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        commentId,
        playerId: storedPlayerId
      });
    }
  };

  const handleNextGame = async () => {
    try {
      const playerId = localStorage.getItem('playerId');
      const playerName = localStorage.getItem('playerName');
      
      const payload = {
        playerId: playerId,
        playerName: playerName
      };

      const response = await axios.post('http://localhost:8080/api/rooms/join', payload);
      console.log('Joined new room:', response.data);
      
      // Reset all necessary states before navigating
      setGameStage(STAGES.WAITING);
      setTimeLeft(60);
      setComments([]);
      setLikedComments(new Set());
      setLeaderboard([]);
      setComment('');
      setCommentingStarted(false);
      hasSubmittedRef.current = false;
      if (timerIdRef.current) {
        clearInterval(timerIdRef.current);
      }
      
      // Navigate to the new room
      navigate(`/room/${response.data.roomId}`);
    } catch (error) {
      console.error('Error joining new room:', error);
    }
  };

  const handleLeave = () => {
    sessionStorage.clear(); // Clear session storage instead of local storage
    navigate('/');
  };

  useEffect(() => {
    if (gameStage === STAGES.COMMENTING) {
      startCommentingPhase();
    } else if (gameStage === STAGES.LIKING) {
      startLikingPhase();
    }
  }, [gameStage]);

  const submitComment = async () => {
    if (hasSubmittedRef.current) return;

    try {
      const blob = await new Promise(resolve => {
        canvas.getElement().toBlob(resolve, 'image/jpeg', 0.9);
      });
      
      const fileName = `meme-${roomId}-${Date.now()}.jpg`;
      const imageUrl = await uploadToS3(blob, fileName);
      
      // Use sessionStorage instead of localStorage
      sessionStorage.setItem(`meme-${roomId}`, imageUrl);

      // Get player info from sessionStorage
      const storedPlayerId = sessionStorage.getItem('playerId');
      const storedPlayerName = sessionStorage.getItem('playerName');

      console.log('Submitting comment with:', {
        playerId: storedPlayerId,
        playerName: storedPlayerName,
        content: imageUrl
      });

      await axios.post(
        `http://localhost:8080/api/comments/room/${roomId}`, 
        {
          content: imageUrl
        },
        {
          params: {
            playerId: storedPlayerId,
            playerName: storedPlayerName
          }
        }
      );

      hasSubmittedRef.current = true;
    } catch (error) {
      console.error('Error submitting meme:', error);
    }
  };

  // Update cleanupS3Images to use sessionStorage
  const cleanupS3Images = async () => {
    const memeUrl = sessionStorage.getItem(`meme-${roomId}`);
    if (memeUrl) {
      try {
        await deleteFromS3(memeUrl);
        sessionStorage.removeItem(`meme-${roomId}`);
      } catch (error) {
        console.error('Error cleaning up S3 images:', error);
      }
    }
  };

  useEffect(() => {
    return () => {
      cleanupS3Images();
      if (timerIdRef.current) {
        clearInterval(timerIdRef.current);
      }
    };
  }, []);

  // Add this function to initialize the canvas
  const initCanvas = async (imageUrl) => {
    if (!canvasRef.current) return;

    const fabricCanvas = new Canvas(canvasRef.current, {
      width: 600,
      height: 600
    });

    // Create a new HTML Image element
    const img = new Image();
    img.crossOrigin = "anonymous"; // Important for CORS
    img.src = imageUrl;

    // Wait for image to load
    img.onload = () => {
      const fabricImg = new FabricImage(img, {
        scaleX: 600 / img.width,
        scaleY: 600 / img.height,
        selectable: false
      });
      
      fabricCanvas.add(fabricImg);
      fabricCanvas.centerObject(fabricImg);
      fabricCanvas.renderAll();
    };

    setCanvas(fabricCanvas);
  };

  // Add useEffect to initialize canvas when gameImage changes
  useEffect(() => {
    if (gameImage && canvasRef.current) {
      initCanvas(gameImage);
    }
  }, [gameImage]);

  // Update the text addition button
  const addTextToCanvas = () => {
    if (!canvas || !memeText.trim()) return;

    const text = new Text(memeText.trim(), {
      left: 300,
      top: 300, // Center of canvas
      fontSize: 48,
      fontFamily: 'Impact', // Classic meme font
      fill: 'white',
      stroke: 'black',
      strokeWidth: 2,
      textAlign: 'center',
      originX: 'center',
      originY: 'center',
      fontWeight: 'bold'
    });
    
    // Make text draggable
    text.set({
      selectable: true,
      hasControls: true,
      hasBorders: true
    });
    
    canvas.add(text);
    canvas.renderAll();
    setMemeText('');
  };

  // Update the commenting phase JSX
  const renderCommentingPhase = () => (
    <div className="instagram-post">
      <div className="timer">
        <span className="timer-icon">⏱️</span>
        <span className="timer-text">{formatTime(timeLeft)}</span>
      </div>
      <div className="centered-content">
        <div className="instagram-header">
          <h2>Create Your Meme!</h2>
        </div>
        <div className="meme-editor">
          <canvas ref={canvasRef} />
          <div className="meme-controls">
            <input
              type="text"
              value={memeText}
              onChange={(e) => setMemeText(e.target.value)}
              placeholder="Enter text..."
              className="meme-text-input"
            />
            <div className="editor-buttons">
              <button
                onClick={addTextToCanvas}
                disabled={!canvas || !memeText.trim()}
                className="add-text-button"
              >
                Add Text
              </button>
              <button
                onClick={submitComment}
                disabled={hasSubmittedRef.current || timeLeft === 0}
                className="submit-button"
              >
                Submit Meme
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div>
      <BubbleBackground />
      <div className="game-room-container">
        {isTransitioning ? (
          <div className="transition-screen">
            <div className="loading-spinner"></div>
            <h2>Getting all memes ready...</h2>
          </div>
        ) : (
          <Box 
            className="content-container"
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '100vh',
              position: 'relative',
              zIndex: 1
            }}
          >
            {gameStage === STAGES.WAITING && (
              <div className="waiting-room">
                <div className="players-list">
                  <h3>Waiting for players... ({players.length}/4)</h3>
                  {players.map(p => (
                    <div key={p.id} className="player-item">
                      <div 
                        className="player-avatar"
                        style={{
                          backgroundColor: getPlayerColor(p.id),
                          width: '40px',
                          height: '40px',
                          borderRadius: '50%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: 'white',
                          fontWeight: 'bold',
                          fontSize: '18px'
                        }}
                      >
                        {p.username ? p.username[0].toUpperCase() : '?'}
                      </div>
                      <div className="player-username">
                        {p.username || 'Unknown Player'}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {gameStage === STAGES.COMMENTING && (
              <div className="instagram-post">
                <div className="timer">
                  <span className="timer-icon">⏱️</span>
                  <span className="timer-text">{formatTime(timeLeft)}</span>
                </div>
                <div className="centered-content">
                  <div className="instagram-header">
                    <h2>Create Your Meme!</h2>
                  </div>
                  <div className="meme-editor">
                    <canvas ref={canvasRef} />
                    <div className="meme-controls">
                      <input
                        type="text"
                        value={memeText}
                        onChange={(e) => setMemeText(e.target.value)}
                        placeholder="Enter text..."
                        className="meme-text-input"
                      />
                      <div className="editor-buttons">
                        <button
                          onClick={addTextToCanvas}
                          disabled={!canvas || !memeText.trim()}
                          className="add-text-button"
                        >
                          Add Text
                        </button>
                        <button
                          onClick={submitComment}
                          disabled={hasSubmittedRef.current || timeLeft === 0}
                          className="submit-button"
                        >
                          Submit Meme
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {gameStage === STAGES.LIKING && (
              <div className="instagram-post">
                <div className="timer">
                  <span className="timer-icon">⏱️</span>
                  <span className="timer-text">{formatTime(timeLeft)}</span>
                </div>
                <div className="centered-content">
                  <div className="instagram-header">
                    <h2>Like Your Favorite Memes!</h2>
                  </div>
                  <div className="memes-grid">
                    {comments && comments.length > 0 ? (
                      comments
                        .filter(comment => comment.playerId !== player.id)
                        .map(comment => (
                          <Card 
                            key={comment.commentId}
                            className="meme-card"
                            elevation={3}
                          >
                            <CardContent>
                              <Typography variant="subtitle2" className="author-name">
                                {comment.playerName || 'Unknown Player'}
                              </Typography>
                              <div className="meme-image-container">
                                <img 
                                  src={comment.content} 
                                  alt={`meme by ${comment.playerName || 'Unknown Player'}`}
                                  className="meme-image"
                                />
                              </div>
                              <IconButton
                                onClick={() => handleLike(comment.commentId)}
                                color="error"
                                disabled={likedComments.has(comment.commentId)}
                                className="like-button"
                              >
                                {likedComments.has(comment.commentId) ? 
                                  <FavoriteIcon /> : 
                                  <FavoriteBorderIcon />
                                }
                              </IconButton>
                            </CardContent>
                          </Card>
                        ))
                    ) : (
                      <Typography variant="h6" className="no-memes-message">
                        No memes to display
                      </Typography>
                    )}
                  </div>
                </div>
              </div>
            )}

            {gameStage === STAGES.RESULTS && (
              <Box className="results-container">
                {leaderboard.winningComment && (
                  <Card className="winner-section" elevation={6}>
                    <CardContent>
                      <Typography variant="h4" gutterBottom>
                        🏆 Winner 🏆
                      </Typography>
                      <Typography variant="h5" className="winner-name">
                        {leaderboard.winningComment.playerName || 'Unknown Player'}
                      </Typography>
                      <div className="winner-meme-container">
                        <img 
                          src={leaderboard.winningComment.content}
                          alt="winning meme"
                          className="winner-meme"
                        />
                      </div>
                      <Box display="flex" alignItems="center" justifyContent="center" mt={2}>
                        <FavoriteIcon color="error" />
                        <Typography variant="h6" ml={1}>
                          {leaderboard.winningComment.likes}
                        </Typography>
                      </Box>
                    </CardContent>
                  </Card>
                )}
                
                <Typography variant="h5" gutterBottom mt={4}>
                  All Memes
                </Typography>
                <Box className="leaderboard">
                  {leaderboard.leaderboard && leaderboard.leaderboard.map((entry, index) => (
                    <Card 
                      key={entry.commentId} 
                      className="leaderboard-item"
                      elevation={3}
                    >
                      <CardContent>
                        <Box className="leaderboard-header">
                          <Typography variant="h6" className="rank">
                            #{index + 1}
                          </Typography>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {entry.playerName || 'Unknown Player'}
                          </Typography>
                        </Box>
                        <div className="leaderboard-meme-container">
                          <img 
                            src={entry.content}
                            alt={`meme by ${entry.playerName || 'Unknown Player'}`}
                            className="leaderboard-meme"
                          />
                        </div>
                        <Box display="flex" alignItems="center" justifyContent="flex-end" mt={2}>
                          <FavoriteIcon color="error" />
                          <Typography variant="subtitle1" ml={1}>
                            {entry.likes}
                          </Typography>
                        </Box>
                      </CardContent>
                    </Card>
                  ))}
                </Box>
                <div className="action-buttons">
                  <button onClick={handleNextGame} className="next-game-button">
                    Next Game
                  </button>
                  <button onClick={handleLeave} className="leave-button">
                    Leave Game
                  </button>
                </div>
              </Box>
            )}
          </Box>
        )}
      </div>
    </div>
  );
}

export default GameRoom; 