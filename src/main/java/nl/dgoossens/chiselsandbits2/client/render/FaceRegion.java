package nl.dgoossens.chiselsandbits2.client.render;

import net.minecraft.util.Direction;

public class FaceRegion {
    private final Direction face;
    private final int state;
    private final boolean isEdge;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public FaceRegion(final Direction myFace, final int centerX, final int centerY, final int centerZ, final int myState, final boolean isEdgeFace) {
        face = myFace;
        state = myState;
        isEdge = isEdgeFace;
        minX = centerX;
        minY = centerY;
        minZ = centerZ;
        maxX = centerX;
        maxY = centerY;
        maxZ = centerZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean isEdge() {
        return isEdge;
    }

    public int getState() {
        return state;
    }

    public Direction getFace() {
        return face;
    }

    boolean extend(final FaceRegion currentFace) {
        if (currentFace.state != state)
            return false;

        switch (face) {
            case DOWN:
            case UP: {
                final boolean a = maxX == currentFace.minX - 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean b = minX == currentFace.maxX + 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean c = maxZ == currentFace.minZ - 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean d = minZ == currentFace.maxZ + 2 && maxX == currentFace.maxX && minX == currentFace.minX;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case WEST:
            case EAST: {
                final boolean a = maxY == currentFace.minY - 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean b = minY == currentFace.maxY + 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean c = maxZ == currentFace.minZ - 2 && maxY == currentFace.maxY && minY == currentFace.minY;
                final boolean d = minZ == currentFace.maxZ + 2 && maxY == currentFace.maxY && minY == currentFace.minY;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case NORTH:
            case SOUTH: {
                final boolean a = maxY == currentFace.minY - 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean b = minY == currentFace.maxY + 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean c = maxX == currentFace.minX - 2 && maxY == currentFace.maxY && minY == currentFace.minY;
                final boolean d = minX == currentFace.maxX + 2 && maxY == currentFace.maxY && minY == currentFace.minY;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            default:
                return false;
        }
    }
}