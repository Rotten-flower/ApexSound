import dev.flower.apex_sound_effects.client.TargetLabelProjection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

public final class TargetLabelProjectionCheck {
    private static int checks;
    private static void expect(boolean value) {checks++;if(!value)throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        var matrix=new Matrix4f().perspective((float)Math.toRadians(70),16f/9f,.05f,256);
        var scratch=new Vector4f();var screen=new Vector2f();
        expect(TargetLabelProjection.project(matrix,0,0,-8,1280,720,scratch,screen));
        expect(Math.abs(screen.x-640)<.001 && Math.abs(screen.y-360)<.001);
        expect(TargetLabelProjection.project(matrix,-2,1,-8,1280,720,scratch,screen));
        expect(screen.x<640 && screen.y<360);
        float nearX=screen.x;
        expect(TargetLabelProjection.project(matrix,-2,1,-16,1280,720,scratch,screen));
        expect(screen.x>nearX && screen.x<640);
        expect(!TargetLabelProjection.project(matrix,0,0,8,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,0,0,0,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,0,0,-.01,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,0,0,-300,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,100,0,-8,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,0,100,-8,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,Double.NaN,0,-8,1280,720,scratch,screen));
        expect(!TargetLabelProjection.project(matrix,0,0,-8,0,720,scratch,screen));
        // Minecraft yaw 0 looks toward +Z; the world view uses yaw + 180 degrees.
        var forward=new Matrix4f(matrix).rotateY((float)Math.PI);
        expect(TargetLabelProjection.project(forward,0,0,8,640,360,scratch,screen));
        expect(Math.abs(screen.x-320)<.001 && Math.abs(screen.y-180)<.001);
        expect(!TargetLabelProjection.project(forward,0,0,-8,640,360,scratch,screen));
        expect(TargetLabelProjection.upperLeft(matrix,0,1.4,-8,1.4f,1.4f,1280,720,scratch,screen));
        float boxX=screen.x,boxY=screen.y;
        expect(boxX<640 && boxY<360);
        double pixelsPerTangent=360/Math.tan(Math.toRadians(35));
        expect(Math.abs(boxX-(640-.7/7.3*pixelsPerTangent))<.01);
        expect(Math.abs(boxY-(360-1.4/7.3*pixelsPerTangent))<.01);
        expect(TargetLabelProjection.upperLeft(matrix,0,1.4,-8,3,1.4f,1280,720,scratch,screen));
        expect(screen.x<boxX);
        expect(TargetLabelProjection.upperLeft(matrix,0,3,-8,1.4f,3,1280,720,scratch,screen));
        expect(screen.y<boxY);
        expect(!TargetLabelProjection.upperLeft(matrix,0,1.4,8,1.4f,1.4f,1280,720,scratch,screen));
        expect(!TargetLabelProjection.upperLeft(matrix,100,1.4,-8,1.4f,1.4f,1280,720,scratch,screen));
        expect(TargetLabelProjection.upperLeft(matrix,10,1.4,-8,1.4f,1.4f,1280,720,scratch,screen));
        expect(!TargetLabelProjection.upperLeft(matrix,0,1.4,-8,0,1.4f,1280,720,scratch,screen));
        expect(!TargetLabelProjection.upperLeft(matrix,0,1.4,-8,1.4f,Float.NaN,1280,720,scratch,screen));
        expect(!TargetLabelProjection.upperLeft(matrix,0,1.4,-.1,1.4f,1.4f,1280,720,scratch,screen));
        System.out.println("PASS: "+checks+" projection, motion, GUI scale and clipping checks");
    }
}
