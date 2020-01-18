package nl.dgoossens.chiselsandbits2.common.chiseledblock.iterators.bit;

import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class BitOcclusionIterator extends BitCollisionIterator {
    private final List<AxisAlignedBB> o;
    private final double epsilon = 0.00001;
    private final double epsilonGap = epsilon * 2.1;
    private final double xFullMinusEpsilon = 1.0 - epsilon;
    private float physicalStartX = 0.0f;
    private boolean lastSetting = false;

    public BitOcclusionIterator(final List<AxisAlignedBB> out) {
        o = out;
    }

    @Override
    protected void yPlus() {
        addCurrentBox(one16th);
        super.yPlus();
    }

    @Override
    protected void zPlus() {
        addCurrentBox(one16th);
        super.zPlus();
    }

    @Override
    protected void done() {
        addCurrentBox(one16th);
    }

    protected void addCurrentBox(final double addition) {
        if (lastSetting == true) {
            addBox(addition);
            lastSetting = false;
        }
    }

    private void addBox(final double addition) {
        final AxisAlignedBB newBox = new AxisAlignedBB(
                physicalStartX < epsilon ? physicalStartX : physicalStartX + epsilon,
                y == 0 ? physicalY : physicalY + epsilon,
                z == 0 ? physicalZ : physicalZ + epsilon,
                physicalX + addition > xFullMinusEpsilon ? physicalX + addition : physicalX + addition - epsilon,
                y == 15 ? physicalYp1 : physicalYp1 - epsilon,
                z == 15 ? physicalZp1 : physicalZp1 - epsilon);

        if (!o.isEmpty()) {
            int offset = o.size() - 1;
            AxisAlignedBB lastBox = o.get(offset);

            if (isBelow(newBox, lastBox)) {
                AxisAlignedBB combined = lastBox.union(newBox);
                o.remove(offset);

                if (!o.isEmpty()) {
                    offset = o.size() - 1;
                    lastBox = o.get(offset);
                    if (!o.isEmpty() && isNextTo(combined, lastBox)) {
                        combined = lastBox.union(combined);
                        o.remove(offset);
                    }

                }

                o.add(combined);
                return;
            }
        }

        o.add(newBox);
    }

    private boolean isNextTo(final AxisAlignedBB newBox, final AxisAlignedBB lastBox) {
        final double touchingZ = newBox.minZ - lastBox.maxZ;
        if (touchingZ >= epsilonGap) return false;
        return bbTest(newBox, lastBox);
    }

    private boolean isBelow(final AxisAlignedBB newBox, final AxisAlignedBB lastBox) {
        final double touchingY = newBox.minY - lastBox.maxY;
        if (touchingY >= 0.001) return false;
        return bbTest(newBox, lastBox);
    }

    private boolean bbTest(final AxisAlignedBB newBox, final AxisAlignedBB lastBox) {
        final boolean sameX = newBox.minX == lastBox.minX && newBox.maxX == lastBox.maxX;
        final boolean sameZ = newBox.minZ == lastBox.minZ && newBox.maxZ == lastBox.maxZ;
        return sameX && sameZ;
    }

    public void add() {
        if (!lastSetting) {
            physicalStartX = physicalX;
            lastSetting = true;
        }
    }

    public void drop() {
        addCurrentBox(0);
    }
}
