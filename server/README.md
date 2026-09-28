# Blockhaven Dedicated Multiplayer Server

A fast, lightweight WebSocket dedicated server for the Blockhaven 3D voxel game.

## Quick Start (Local)

```bash
cd server
npm install
npm start
```

Default server port: `8080`.

## Free / Cheap Hosting Deployment Guide

### Option 1: Render.com (Free Tier)
1. Fork or push the repo to GitHub.
2. Sign in to [Render](https://render.com) and click **New +** -> **Web Service**.
3. Select your repository and set Root Directory to `server`.
4. Environment: `Node`, Build Command: `npm install`, Start Command: `node server.js`.
5. Your public server URL will be: `wss://<your-service>.onrender.com`.

### Option 2: Fly.io or Railway.app
- Deploy using the included `Dockerfile`:
```bash
fly launch
fly deploy
```

### Option 3: Local Hotspot / Wi-Fi LAN
- Connect both Android phones to the same Wi-Fi or mobile hotspot.
- One player hosts directly from their IP or starts the server locally.
- In-game, enter the IP (e.g. `192.168.1.50:8080`) and Room Code.
