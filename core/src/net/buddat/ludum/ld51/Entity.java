package net.buddat.ludum.ld51;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;

import java.util.HashMap;

public class Entity
{

    /*
    Entity Action
        Needs:
            Action Type
            Animation
            Icon
            Effects - Abstract?
            Loop Offset
            Time Since Last Trigger
            Active Time - Min/Max?
                Get/Set Active for things like Jump where the time is variable
     */
    public static final String IDLE_STRING = "idle";

    public static final String MOVE_STRING = "move";

    public static final String JUMP_STRING = "jump";

    public static final String PORTAL_STRING = "portal";

    public static final float MOVEMENT_SPEED = 8f;

    public static final float DASH_SPEED = 35f;

    public static final float JUMP_FORCE = 11f;

    private String name;

    private Body physicsBody;

    private HashMap<String, Animation<TextureRegion>> animationMap = new HashMap<>();

    private SpriteBatch spriteBatch;

    private String currentAnimation;

    private boolean currentAnimationLoops = true;

    private float animationState;

    private boolean flip = false;

    private boolean spawning = true;

    public Entity(String name, String idleAnimation, int colCount, int rowCount)
    {
        this.name = name;

        loadAnimation(IDLE_STRING, idleAnimation, colCount, rowCount);
        currentAnimation = IDLE_STRING;

        spriteBatch = new SpriteBatch();
    }

    public void loadAnimation(String animationName, String spritesheet, int colCount, int rowCount)
    {
        Texture loadedSheet = new Texture(spritesheet);
        TextureRegion[][] tempRegions = TextureRegion.split(loadedSheet, loadedSheet.getWidth() / colCount,
                loadedSheet.getHeight() / rowCount);

        TextureRegion[] frames = new TextureRegion[colCount * rowCount];
        int idx = 0;
        for (int i = 0; i < rowCount; i++)
            for (int j = 0; j < colCount; j++)
                frames[idx++] = tempRegions[i][j];

        Animation<TextureRegion> loadedAnimation = new Animation<TextureRegion>(0.25f, frames);
        animationMap.put(animationName, loadedAnimation);
    }

    public void setPhysicsBody(Body b)
    {
        this.physicsBody = b;
        this.physicsBody.setFixedRotation(true);
    }

    public void setPosition(int xLoc, int yLoc, boolean cancelMomentum)
    {
        physicsBody.setTransform(xLoc, yLoc, physicsBody.getAngle());

        if (cancelMomentum)
        {
            physicsBody.setLinearVelocity(0f, 0f);
            physicsBody.setAngularVelocity(0f);
        }

        physicsBody.setAwake(true);
    }

    public void playSpawn()
    {
        spawning = true;
    }

    public void jump(float multiplier)
    {
        // physicsBody.applyForceToCenter(0f, 500f, true);
        physicsBody.applyLinearImpulse(0f, physicsBody.getMass() * JUMP_FORCE * multiplier, physicsBody.getWorldCenter().x,
                physicsBody.getWorldCenter().y, true);
    }

    public void slam(float multiplier)
    {
        physicsBody.applyLinearImpulse(0f, physicsBody.getMass() * (-JUMP_FORCE) * multiplier, physicsBody.getWorldCenter().x,
                physicsBody.getWorldCenter().y, true);
    }

    public void moveRight(boolean onGround)
    {
        flip = false;
        float xChange = MOVEMENT_SPEED - physicsBody.getLinearVelocity().x;
        physicsBody.applyLinearImpulse(physicsBody.getMass() * xChange * (onGround ? 1f : 0.05f), 0f,
                physicsBody.getWorldCenter().x, physicsBody.getWorldCenter().y, true);
    }

    public void moveLeft(boolean onGround)
    {
        flip = true;
        float xChange = (-MOVEMENT_SPEED) - physicsBody.getLinearVelocity().x;
        physicsBody.applyLinearImpulse(physicsBody.getMass() * xChange * (onGround ? 1f : 0.05f), 0f,
                physicsBody.getWorldCenter().x, physicsBody.getWorldCenter().y, true);
    }

    public void dash(float multiplier)
    {
        float xChange = (DASH_SPEED * (flip ? -1f : 1f)) - physicsBody.getLinearVelocity().x;
        physicsBody.applyLinearImpulse(physicsBody.getMass() * xChange * multiplier, 0f, physicsBody.getWorldCenter().x,
                physicsBody.getWorldCenter().y, true);
    }

    public float getPosX()
    {
        return physicsBody.getPosition().x;
    }

    public float getPosY()
    {
        return physicsBody.getPosition().y;
    }

    public void slowDown(boolean onGround)
    {
        float xChange = 0;
        if (onGround)
            xChange = -physicsBody.getLinearVelocity().x;
        else
        {
            if (physicsBody.getLinearVelocity().x < -MOVEMENT_SPEED)
                xChange = MOVEMENT_SPEED;
            else if (physicsBody.getLinearVelocity().x > MOVEMENT_SPEED)
                xChange = -MOVEMENT_SPEED;
        }

        if (xChange == 0)
            return;

        physicsBody.applyLinearImpulse(physicsBody.getMass() * xChange / 5f, 0f, physicsBody.getWorldCenter().x,
                physicsBody.getWorldCenter().y, true);
    }

    public void renderEntity(float delta, OrthographicCamera camera, boolean onGround, boolean paused)
    {
        if (!paused)
            animationState += delta;

        boolean skipChange = false;
        if (spawning)
        {
            changeAnimation(PORTAL_STRING, false);
            if (!animationMap.get(currentAnimation).isAnimationFinished(animationState))
                skipChange = true;
            else
                spawning = false;
        }
        if (!skipChange)
        {
            if (!onGround)
                changeAnimation(JUMP_STRING, false);
            else if (Math.abs(physicsBody.getLinearVelocity().x) >= 1f)
                changeAnimation(MOVE_STRING, true);
            else
                changeAnimation(IDLE_STRING, true);
        }

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
            TextureRegion frame = animationMap.get(currentAnimation).getKeyFrame(animationState, currentAnimationLoops);
            if (frame.isFlipX() != flip)
                frame.flip(true, false);
            spriteBatch.draw(frame, physicsBody.getPosition().x * 8f - frame.getRegionWidth() / 2f,
                    physicsBody.getPosition().y * 8f - frame.getRegionHeight() / 8f); // because sprites are a weird size
        spriteBatch.end();
    }

    private void changeAnimation(String toAnim, boolean loops)
    {
        if (currentAnimation == toAnim)
            return;

        currentAnimation = toAnim;
        currentAnimationLoops = loops;
        animationState = 0f;
    }

    public void dispose()
    {
        spriteBatch.dispose();
    }

}
