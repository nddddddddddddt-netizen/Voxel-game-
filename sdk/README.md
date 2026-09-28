# Blockhaven Modding SDK (`bh`)

Official Modding SDK and Developer Toolkit for **Blockhaven** (Android 3D Voxel Sandbox).

## Supported Languages & Environments
- **Level 1 (Safe Sandbox - Default)**:
  - TypeScript / JavaScript (via embedded QuickJS engine)
  - Lua (isolated environment)
  - Rust / C / C++ / Zig / AssemblyScript (compiled to WebAssembly `.wasm`)
  - Kotlin / Java (compiled to WASM via TeaVM or Kotlin/Wasm)
- **Level 2 (Power Mode - Optional, Enabled in Settings > Developer)**:
  - Native arm64-v8a `.so` libraries (`System.load`)
  - DEX / JAR bytecode loading (`DexClassLoader`)
  - Embedded CPython interpreter for Android

## Package Specification (`.bhmod`)
A `.bhmod` is a zip package containing:
1. `mod.json` (Manifest file)
2. `main.wasm` or `main.js` or `main.so` or `main.dex`
3. `assets/` (Textures, block models, audio clips)

### Manifest Example (`mod.json`)
```json
{
  "modId": "custom_lumina",
  "name": "Lumina Crystals",
  "version": "1.0.0",
  "author": "VoxelCrafter",
  "description": "Adds radiant crystal ore veins and luminescent wands.",
  "level": "LEVEL_1_SAFE_SANDBOX",
  "language": "TypeScript",
  "permissions": ["world.blocks", "custom.items", "chat.commands"],
  "sha256": "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a"
}
```

## Modding API Lifecycle Hooks (JavaScript / TypeScript / WASM)
```typescript
export function onBlockBreak(x: number, y: number, z: number, blockId: number, player: Player): boolean {
  console.log(`Block ${blockId} broken at (${x}, ${y}, ${z})`);
  return false; // Return true to cancel the event
}

export function onCommand(cmd: string, args: string[], player: Player): string | null {
  if (cmd === "light") {
    player.setAir(10);
    return "Aura of light bestowed upon player!";
  }
  return null;
}
```
