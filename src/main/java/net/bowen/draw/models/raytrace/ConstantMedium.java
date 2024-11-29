package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;

import java.nio.ByteBuffer;

public class ConstantMedium extends RaytraceModel {
    private final RaytraceModel boundary;
    private final float density;
    private final Material phaseFunction;

    public ConstantMedium(RaytraceModel boundary, float density, Material phaseFunction) {
        super(phaseFunction);
        this.boundary = boundary;
        this.density = density;
        this.phaseFunction = super.material;

        addModel(boundary);
        bbox = boundary.boundingBox();
    }

    public RaytraceModel getBoundary() {
        return boundary;
    }

    public void putToBuffer(ByteBuffer buffer) {
        // model index in its list
        buffer.putInt(boundary.indexInList);
        // model type
        buffer.putInt(boundary.getModelId());
        // negative inverted density
        buffer.putFloat(-1f/density);
        // phase function material
        buffer.putInt(phaseFunction.getMaterialPackedValue());
        // texture id
        buffer.putInt(phaseFunction.getTexturePackedValue());
    }

    @Override
    protected int getModelId() {
        return CONSTANT_MEDIUM_ID;
    }
}
