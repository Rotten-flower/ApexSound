package dev.flower.apex_sound_effects.client;

import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;

/** 将目标包围盒锚点投影至屏幕坐标。 / Projects a camera-relative entity-box anchor into screen coordinates. */
public final class TargetLabelProjection {
    private TargetLabelProjection() {}

    public static boolean project(Matrix4fc matrix, double x, double y, double z, int width, int height,
                                  Vector4f scratch, Vector2f screen) {
        if (width <= 0 || height <= 0 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        matrix.transform(scratch.set((float) x, (float) y, (float) z, 1));
        if (!Float.isFinite(scratch.w) || scratch.w <= 0.0001f) return false;
        float nx = scratch.x / scratch.w, ny = scratch.y / scratch.w, nz = scratch.z / scratch.w;
        if (!Float.isFinite(nx) || !Float.isFinite(ny) || !Float.isFinite(nz)
                || Math.abs(nx) > 1 || Math.abs(ny) > 1 || nz < -1 || nz > 1) return false;
        screen.set((nx + 1) * width * 0.5f, (1 - ny) * height * 0.5f);
        return true;
    }

    public static boolean upperLeft(Matrix4fc matrix, double x, double top, double z, float bodyWidth, float bodyHeight,
                                    int width, int height, Vector4f scratch, Vector2f screen) {
        if (width <= 0 || height <= 0 || !Double.isFinite(x) || !Double.isFinite(top) || !Double.isFinite(z)
                || !Float.isFinite(bodyWidth) || !Float.isFinite(bodyHeight) || bodyWidth <= 0 || bodyHeight <= 0) return false;
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            float cx = (float) (x + ((corner & 1) == 0 ? -0.5 : 0.5) * bodyWidth);
            float cy = (float) (top - ((corner & 2) == 0 ? 0 : bodyHeight));
            float cz = (float) (z + ((corner & 4) == 0 ? -0.5 : 0.5) * bodyWidth);
            matrix.transform(scratch.set(cx, cy, cz, 1));
            if (!Float.isFinite(scratch.w) || scratch.w <= 0.0001f) return false;
            float nx = scratch.x / scratch.w, ny = scratch.y / scratch.w, nz = scratch.z / scratch.w;
            if (!Float.isFinite(nx) || !Float.isFinite(ny) || !Float.isFinite(nz) || nz < -1 || nz > 1) return false;
            float sx = (nx + 1) * width * 0.5f, sy = (1 - ny) * height * 0.5f;
            minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
            minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
        }
        if (maxX < 0 || minX > width || maxY < 0 || minY > height) return false;
        screen.set(minX, minY);
        return true;
    }
}
