# Minecraft-ChunkViewer-Plugin

Minecraft Paper plugin for retrieving basic chunk and player information from a minecraft server

## Versions
 - Minecraft 1.20.1
 - Java 20 SDK

## Purpose

This project was created to be able to export the top most row of blocks from the world surrounding a player in order to generate and display a height map.

It fires up a jetty http server listening on port 8090 (sorry, no configuration) and answers requests to /get_player_data and /get_chunk_data, if providing the required arguments.

## API Routes

All API routes take JSON as input and return JSON as output.

### <key>POST</key> /get_player_data

Input:

    {
        "name": "<Player name>"
    }

Output:

    {
        "world": string,
        "cx": int,
        "cz": int,
        "x": double,
        "y": double,
        "z": double,
        "yaw": double,
        "pitch": double,
    }

### <key>POST</key> /get_available_chunks

The world, usually "world", can be retrieved from the player data struct.

Input:

    {
        "world": string
    }

Output:

    {
        "chunks": [
              {
                  "x": int,
                  "z": int,
              },
              ...        
        ],
    }

A list of all chunks that are currently available in the world. May differ from the ground truth, depending on how many players are online and how many chunks have been loaded/unloaded in that time.

### <key>POST</key> /get_chunk_data

The world, usually "world", can be retrieved from the player data struct.

Input:

    {
        "world": string,
        "cx": int,
        "cz": int,
    }

Output:

    {
        "height": int[16][16],
        "blocks": string[16][16],
    }

Returned are two 16x16 arrays, one containing the height of the topmost block in each column and one containing the block type of the topmost block in each column.

## Caveats
- It's quite slow.
  It takes about 80 seconds to download a 24x24 area of chunks on my computer.
  I'm not sure, if there's a faster way without loading the chunks directly from the file system.
- It's insecure
  The plugin will load any chunk requested without authorization listening on all IP interfaces and there is no API rate limiting mechanism in place, but I think the plugin can occupy at most one cpu thread.
  At least this plugin can't do any harm to your world or player data, except for maybe crashing the server.
