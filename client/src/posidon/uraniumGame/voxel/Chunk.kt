package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.uranium.voxel.VoxelChunk

class Chunk(
    position: Vec3i,
    chunkMap: ChunkMap
) : VoxelChunk<Block>(position, chunkMap)