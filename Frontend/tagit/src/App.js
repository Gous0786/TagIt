import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import GameRoom from './pages/GameRoom';
import { GameProvider } from './context/GameContext';

function App() {
  return (
    <Router>
      <GameProvider>
        <div className="App">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/room/:roomId" element={<GameRoom />} />
          </Routes>
        </div>
      </GameProvider>
    </Router>
  );
}

export default App; 