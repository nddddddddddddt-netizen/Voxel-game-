/**
 * Blockhaven Authoritative Dedicated Server
 * Handles multi-client synchronization, block deltas, rooms, and chat.
 */

const { WebSocketServer } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ status: 'ok', game: 'Blockhaven', version: '1.0.0' }));
});

const wss = new WebSocketServer({ server });

// Room state: roomCode -> { players: Map, deltas: Array, seed: Number }
const rooms = new Map();

function getOrCreateRoom(roomCode) {
  if (!rooms.has(roomCode)) {
    rooms.set(roomCode, {
      players: new Map(),
      deltas: [],
      seed: Math.floor(Math.random() * 99999999)
    });
  }
  return rooms.get(roomCode);
}

let nextPlayerId = 100;

wss.on('connection', (ws) => {
  let playerRoom = null;
  let playerId = nextPlayerId++;
  let playerName = `Player_${playerId}`;

  ws.on('message', (data) => {
    try {
      const packet = JSON.parse(data.toString());

      switch (packet.type) {
        case 'JOIN': {
          const roomCode = (packet.roomCode || 'DEFAULT').toUpperCase();
          playerRoom = getOrCreateRoom(roomCode);
          playerName = packet.playerName || playerName;

          playerRoom.players.set(playerId, {
            id: playerId,
            name: playerName,
            x: 8, y: 66, z: 8,
            yaw: 0, pitch: 0,
            ws
          });

          // Send join ack
          ws.send(JSON.stringify({
            type: 'JOIN_ACK',
            playerId,
            seed: playerRoom.seed,
            deltas: playerRoom.deltas
          }));

          // Broadcast to other players in the room
          broadcastToRoom(playerRoom, playerId, {
            type: 'PLAYER_JOINED',
            playerId,
            name: playerName
          });
          break;
        }

        case 'MOVE': {
          if (!playerRoom) return;
          const p = playerRoom.players.get(playerId);
          if (p) {
            p.x = packet.x;
            p.y = packet.y;
            p.z = packet.z;
            p.yaw = packet.yaw;
            p.pitch = packet.pitch;

            broadcastToRoom(playerRoom, playerId, {
              type: 'PLAYER_MOVE',
              playerId,
              x: p.x, y: p.y, z: p.z,
              yaw: p.yaw, pitch: p.pitch
            });
          }
          break;
        }

        case 'BLOCK_CHANGE': {
          if (!playerRoom) return;
          const delta = {
            playerId,
            x: packet.x,
            y: packet.y,
            z: packet.z,
            blockId: packet.blockId
          };
          playerRoom.deltas.push(delta);

          // Broadcast block modification to everyone in the room
          broadcastToRoom(playerRoom, null, {
            type: 'BLOCK_DELTA',
            delta
          });
          break;
        }

        case 'CHAT': {
          if (!playerRoom) return;
          broadcastToRoom(playerRoom, null, {
            type: 'CHAT_MSG',
            sender: playerName,
            message: packet.message
          });
          break;
        }
      }
    } catch (err) {
      console.error('Packet parsing error:', err);
    }
  });

  ws.on('close', () => {
    if (playerRoom) {
      playerRoom.players.delete(playerId);
      broadcastToRoom(playerRoom, playerId, {
        type: 'PLAYER_LEFT',
        playerId
      });
      if (playerRoom.players.size === 0) {
        // Clean up empty rooms after inactivity
        setTimeout(() => {
          if (playerRoom.players.size === 0) {
            // Keep deltas or garbage collect
          }
        }, 60000);
      }
    }
  });
});

function broadcastToRoom(room, excludePlayerId, packet) {
  const json = JSON.stringify(packet);
  for (const [id, peer] of room.players) {
    if (id !== excludePlayerId && peer.ws.readyState === 1) {
      peer.ws.send(json);
    }
  }
}

server.listen(PORT, () => {
  console.log(`Blockhaven Authoritative Dedicated Server running on port ${PORT}`);
});
