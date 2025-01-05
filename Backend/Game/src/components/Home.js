import axios from 'axios';

function Home() {
    const handleJoinGame = async () => {
        try {
            const response = await axios.post('http://localhost:8080/api/rooms/join', 
                {
                    // your player data
                    playerId: generatePlayerId(), // implement this function
                    playerName: playerName,
                    // other player properties
                },
                {
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    timeout: 5000 // 5 second timeout
                }
            );
            
            // Handle successful response
            const room = response.data;
            // Navigate to game room or update state
            
        } catch (error) {
            console.error('Error joining room:', error);
            if (error.response) {
                // Server responded with error
                console.error('Server error:', error.response.data);
            } else if (error.request) {
                // Request made but no response
                console.error('No response from server. Is the server running?');
            } else {
                // Something else went wrong
                console.error('Error setting up request:', error.message);
            }
        }
    };

    // Rest of your component code...
} 