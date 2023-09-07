# Minecraft-ChunkViewer-Plugin
Minecraft Paper plugin for retrieving basic chunk and player information from a minecraft server

This project was created to be able to export the top most row of blocks from the world surrounding a player in order to generate and display a height map.

Caveats:
- Slow
- Insecure (will load any chunk requested without authorization listening on all IP interfaces)
- Crashes when stopping the http server on plugin shutdown
