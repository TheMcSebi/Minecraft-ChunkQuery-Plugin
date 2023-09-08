package com.themcsebi.chunkquery;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import spark.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

public final class ChunkQuery extends JavaPlugin {
    final Logger logger = Bukkit.getLogger();

    private static Service http;

    public static void startHttpServer() {
        http = Service.ignite().port(8090);

        http.post("/get_player_data", (req, res) -> {
            res.type("application/json");

            Gson gson = new Gson();
            Map<String, Object> responseData = new HashMap<>();
            String player_name;
            try {
                Map inputData = gson.fromJson(req.body(), Map.class);
                // Extract input data
                player_name = ((String) inputData.get("name"));
            } catch (Exception e) {
                responseData.put("error", "Error parsing input data");
                responseData.put("error_info", e.toString());
                return gson.toJson(responseData);
            }

            Player player = Bukkit.getPlayer(player_name);
            if (player == null) {
                responseData.put("error", "Player not found");
                return gson.toJson(responseData);
            }

            Location loc = player.getLocation();
            String worldName = player.getWorld().getName();
            int cx = loc.getChunk().getX();
            int cz = loc.getChunk().getZ();
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            double yaw = loc.getYaw();
            double pitch = loc.getPitch();

            responseData.put("world", worldName);
            responseData.put("cx", cx);
            responseData.put("cz", cz);
            responseData.put("x", x);
            responseData.put("y", y);
            responseData.put("z", z);
            responseData.put("yaw", yaw);
            responseData.put("pitch", pitch);


            return gson.toJson(responseData);
        });

        /**
         * Get a list of all chunks that are currently loaded and generated in a world
         *
         * Useful for downloading all currently loaded chunks from a server
         */
        http.post("/get_available_chunks", (req, res) -> {
                    res.type("application/json");


            // Get input from request
            Gson gson = new Gson();
            Map<String, Object> responseData = new HashMap<>();
            String world_name;
            try {
                Map inputData = gson.fromJson(req.body(), Map.class);
                // Extract input data
                world_name = ((String) inputData.get("world"));
            } catch (Exception e) {
                responseData.put("error", "Error parsing input data");
                responseData.put("error_info", e.toString());
                return gson.toJson(responseData);
            }

            World world = Bukkit.getWorld(world_name);
            if (world == null) {
                responseData.put("error", "World not found");
                return gson.toJson(responseData);
            }

            List<Map<String, Integer>> chunksList = new ArrayList<>();

            for (Chunk chunk : world.getLoadedChunks()) {
                if (world.isChunkGenerated(chunk.getX(), chunk.getZ())) {
                    Map<String, Integer> chunkMap = new HashMap<>();
                    chunkMap.put("x", chunk.getX());
                    chunkMap.put("z", chunk.getZ());
                    chunksList.add(chunkMap);
                }
            }

            responseData.put("chunks", chunksList);
            return gson.toJson(responseData);
        });

        // Create an endpoint that takes inputs
        http.post("/get_chunk_data", (req, res) -> {
            res.type("application/json");

            // Get input from request (for simplicity, assuming JSON payload)
            Gson gson = new Gson();
            Map<String, Object> responseData = new HashMap<>();
            String cx_str, cz_str, worldName;
            boolean chunkWasForceLoaded = false;

            try {
                Map inputData = gson.fromJson(req.body(), Map.class);
                // Extract input data
                cx_str = inputData.get("cx").toString();
                cz_str = inputData.get("cz").toString();
                worldName = ((String) inputData.get("world"));
            } catch (Exception e) {
                responseData.put("error", "Error parsing input data");
                responseData.put("error_info", e.toString());
                return gson.toJson(responseData);
            }


            int cx, cz;
            try {
                cx = (int)Double.parseDouble(cx_str);;
                cz = (int)Double.parseDouble(cz_str);;
            } catch (Exception e) {
                responseData.put("error", "Error reading chunk coordinates");
                responseData.put("error_info", e.toString());
                return gson.toJson(responseData);
            }

            World world;
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                responseData.put("error", "World not found");
                return gson.toJson(responseData);
            }

            if (!world.isChunkGenerated(cx, cz)) {
                responseData.put("error", "Chunk not generated");
                return gson.toJson(responseData);
            }

            if (!world.isChunkLoaded(cx, cz)) {
                chunkWasForceLoaded = true;
                boolean success = forceLoadChunk(worldName, cx, cz);
                if (!success) {
                    responseData.put("error", "Chunk not currently loaded");
                    return gson.toJson(responseData);
                }
            }

            Chunk chunk = world.getChunkAt(cx, cz);

            Material material;
            int lower_limit = chunk.getWorld().getMinHeight();
            int upper_limit = chunk.getWorld().getMaxHeight() - 1; // 320 is always Material.VOID_AIR, so start at 319
            int current_y;

            String[][] topBlocks = new String[16][16];
            int[][] topBlocksY = new int[16][16];

            // for every x/z coord in the cunk
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    current_y = upper_limit;
                    do {
                        material = chunk.getBlock(dx, current_y, dz).getType();
                        if (material != Material.AIR) {
                            topBlocks[dx][dz] = material.name().toLowerCase();
                            topBlocksY[dx][dz] = current_y;
                            break;
                        }
                    } while(--current_y > lower_limit);
                }
            }

            // needs testing
            // for every x/z coord in the cunk
            //for (int dx = 0; dx < 16; dx++) {
            //    for (int dz = 0; dz < 16; dz++) {
            //        // first check every 8th block downwards
            //        for (current_y = upper_limit; current_y > lower_limit; current_y -= 8) {
            //            if (chunk.getBlock(dx, current_y, dz).getType() != Material.AIR)
            //                break;
            //        }
            //        // move up a little, but watch out to not exceed world limits
            //        current_y = Math.min(current_y + 16, upper_limit);

            //        // then check detailed
            //        Material last_material;
            //        do {
            //            last_material = chunk.getBlock(dx, --current_y, dz).getType();
            //        } while(last_material == Material.AIR);

            //        topBlocks[dx][dz] = last_material.name().toLowerCase();
            //        topBlocksY[dx][dz] = current_y;
            //    }
            //}

            if(chunkWasForceLoaded)
                unloadForceLoadedChunk(worldName, cx, cz);

            // Prepare response data as JSON
            responseData.put("blocks", topBlocks);
            responseData.put("height", topBlocksY);
            return gson.toJson(responseData);
        });
    }

    public static boolean forceLoadChunk(String worldName, int cx, int cy) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        world.getChunkAt(cx, cy).load();
        world.getChunkAt(cx, cy).setForceLoaded(true);
        return true;
    }

    public static boolean unloadForceLoadedChunk(String worldName, int cx, int cy) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        world.getChunkAt(cx, cy).setForceLoaded(false);
        return true;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger.info("[ChunkQuery] Starting HTTP Server");
        new Thread(ChunkQuery::startHttpServer).start();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (http != null) {
            logger.info("[ChunkQuery] Stopping HTTP Server");
            http.stop();
            http.awaitStop();
        }
    }
}
