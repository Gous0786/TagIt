import { createContext, useState, useContext } from 'react';

const GameContext = createContext();

export const GameProvider = ({ children }) => {
  const [player, setPlayer] = useState({
    id: null,
    username: '',
    roomId: null
  });

  const updatePlayer = (data) => {
    setPlayer(prev => ({ ...prev, ...data }));
  };

  return (
    <GameContext.Provider value={{ player, updatePlayer }}>
      {children}
    </GameContext.Provider>
  );
};

export const useGame = () => useContext(GameContext); 