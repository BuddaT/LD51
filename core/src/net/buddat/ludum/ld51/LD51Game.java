package net.buddat.ludum.ld51;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;

public class LD51Game extends ApplicationAdapter implements ContactListener, InputProcessor
{

    private static final boolean RENDER_DEBUG = false;

    private OrthographicCamera mapCamera;

    private OrthographicCamera entityCamera;

    private OrthogonalTiledMapRenderer mapRenderer;

    private Box2DDebugRenderer box2DDebugRenderer;

    private MapManager mapManager;

    private ActionManager actionManager;

    private Entity playerEntity;

    private ArrayList<Fixture> footColliderList = new ArrayList<>();

    private SpriteBatch portalRenderer;

    private Animation<TextureRegion> portalAnimation;

    private float portalAnimationState = 0f;

    private long lastPreRender = System.currentTimeMillis();
    private long lastRender = System.currentTimeMillis();
    private long lastPostRender = System.currentTimeMillis();

    boolean triggerNextMap = false;

    private boolean pauseForActions = false;

    private boolean gameComplete = false;

    private int stage = 0;

    private void init()
    {
        mapManager = new MapManager();
        mapCamera = new OrthographicCamera();
        mapCamera.setToOrtho(false, 40, 20);
        entityCamera = new OrthographicCamera();
        entityCamera.setToOrtho(false, 40 * 8f, 20 * 8f);
        mapManager.loadTiledMap("maps/intro0.tmx", 0, -10);
        mapRenderer = new OrthogonalTiledMapRenderer(mapManager.currentMap, 1 / 8f);
        box2DDebugRenderer = new Box2DDebugRenderer();

        mapManager.currentWorld.setContactListener(this);

        playerEntity = new Entity("player", "entities/soldier/Idle.png", 2, 1);
        PolygonShape s = new PolygonShape();
        s.setAsBox(0.5f, 0.67f);
        playerEntity.setPhysicsBody(mapManager.addDynamicEntity(s, 1f, 0.2f, mapManager.startX, mapManager.startY, true));
        playerEntity.loadAnimation(Entity.MOVE_STRING, "entities/soldier/Run.png", 4, 1);
        playerEntity.loadAnimation(Entity.JUMP_STRING, "entities/soldier/Jump.png", 2, 1);
        playerEntity.loadAnimation(Entity.PORTAL_STRING, "entities/soldier/Spawn.png", 4, 1);

        actionManager = new ActionManager();
        actionManager.init();

        Action jumpAction = new Action(playerEntity, "jump", "actions/jump.png")
        {
            @Override
            public void trigger()
            {
                if (!footColliderList.isEmpty())
                    playerEntity.jump(0.75f);
            }
        };
        actionManager.setAction(jumpAction, -1f);
        Action bigjumpAction = new Action(playerEntity, "big jump", "actions/bigjump.png")
        {
            @Override
            public void trigger()
            {
                if (!footColliderList.isEmpty())
                    playerEntity.jump(1.2f);
            }
        };
        actionManager.setAction(bigjumpAction, 4f);
        Action dashAction = new Action(playerEntity, "dash", "actions/dash.png")
        {
            @Override
            public void trigger()
            {
                playerEntity.dash(0.85f);
            }
        };
        actionManager.setAction(dashAction, -2f);
        Action bigdashAction = new Action(playerEntity, "big dash", "actions/bigdash.png")
        {
            @Override
            public void trigger()
            {
                playerEntity.dash(1.5f);
            }
        };
        actionManager.setAction(bigdashAction, -5f);
        Action slamAction = new Action(playerEntity, "slam", "actions/slam.png")
        {
            @Override
            public void trigger()
            {
                if (!footColliderList.isEmpty())
                    playerEntity.slam(0.75f);
            }
        };
        actionManager.setAction(slamAction, -4f);
        Action bigslamAction = new Action(playerEntity, "big slam", "actions/bigslam.png")
        {
            @Override
            public void trigger()
            {
                if (!footColliderList.isEmpty())
                    playerEntity.slam(1.2f);
            }
        };
        actionManager.setAction(bigslamAction, -7f);

        portalRenderer = new SpriteBatch();
        Texture loadedSheet = new Texture("fx/portal.png");
        TextureRegion[][] tempRegions = TextureRegion.split(loadedSheet, loadedSheet.getWidth() / 5,
                loadedSheet.getHeight() / 3);

        TextureRegion[] frames = new TextureRegion[5 * 3];
        int idx = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 5; j++)
                frames[idx++] = tempRegions[i][j];

        portalAnimation = new Animation<TextureRegion>(0.125f, frames);

        Gdx.input.setInputProcessor(this);
    }

    public void resetMap()
    {
        mapManager.reloadCurrentMap();
        setupMapStuff(false);
        if (stage > 0)
            pauseForActions = true;
    }

    public void nextMap()
    {
        triggerNextMap = true;
    }

    private void setupMapStuff(boolean nextMap)
    {
        mapManager.currentWorld.setContactListener(this);
        mapRenderer.setMap(mapManager.currentMap);

        PolygonShape s = new PolygonShape();
        s.setAsBox(0.5f, 0.67f);
        playerEntity.setPhysicsBody(mapManager.addDynamicEntity(s, 1f, 0.2f, mapManager.startX, mapManager.startY, true));
        playerEntity.playSpawn();
        actionManager.resetTime(true, nextMap);

        footColliderList.clear();
    }

    private void preRender(long delta)
    {
        if (triggerNextMap)
        {
            if (mapManager.nextMap != null)
            {
                mapManager.changeToNextMap();
                setupMapStuff(true);

                pauseForActions = true;
                stage++;
            }
            else
            {
                gameComplete = true;
            }
            triggerNextMap = false;
        }

        if (RENDER_DEBUG)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.O))
                mapCamera.translate(-0.1f, 0f);
            if (Gdx.input.isKeyPressed(Input.Keys.P))
                mapCamera.translate(0.1f, 0f);
            if (Gdx.input.isKeyPressed(Input.Keys.K))
                mapCamera.translate(0f, -0.1f);
            if (Gdx.input.isKeyPressed(Input.Keys.L))
                mapCamera.translate(0f, 0.1f);
        }
        else
        {
            // Maybe move the camera around to track the player, if the map allows it
            float xPos = Math.max(mapCamera.viewportWidth / 2f,
                    Math.min(mapManager.currentMap.getProperties().get("width", 40, Integer.class) - mapCamera.viewportWidth / 2f,
                            playerEntity.getPosX()));
            mapCamera.position.x = xPos;

            float yPos = Math.max(mapCamera.viewportHeight / 2f,
                    Math.min(mapManager.currentMap.getProperties().get("height", 20, Integer.class) - mapCamera.viewportHeight / 2f,
                            playerEntity.getPosY()));
            mapCamera.position.y = yPos;
        }
        mapCamera.update();

        if (pauseForActions == false && gameComplete == false)
        {
            boolean moving = false;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
            {
                playerEntity.moveRight(!footColliderList.isEmpty());
                moving = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
            {
                playerEntity.moveLeft(!footColliderList.isEmpty());
                moving = true;
            }
            if (!moving)
                playerEntity.slowDown(!footColliderList.isEmpty());
            if (Gdx.input.isKeyJustPressed(Input.Keys.R))
                resetMap();

            actionManager.pollActions(delta / 1000f);
        }
        else if (gameComplete)
        {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R))
            {
                mapManager.loadTiledMap("./maps/intro0.tmx", 0, -10);
                mapManager.changeToNextMap();
                setupMapStuff(false);
                stage = 0;
                actionManager.totalTimeTaken = 0f;
                gameComplete = false;
                pauseForActions = true;
            }
        }
        else
        {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
                pauseForActions = false;
        }

        entityCamera.position.set(mapCamera.position.x * 8f, mapCamera.position.y * 8f, 0f);
        entityCamera.update();

        mapRenderer.setView(mapCamera);
    }

    private void renderFrame(long delta)
    {
        // Background render
        // TODO: Parallax background

        // Map render
        mapRenderer.render();

        for (MapManager.PortalInfo p : mapManager.getPortalList())
        {
            portalAnimationState += delta / 1000f;
            portalRenderer.setProjectionMatrix(entityCamera.combined);
            portalRenderer.begin();
            TextureRegion frame = portalAnimation.getKeyFrame(portalAnimationState, true);
            portalRenderer.draw(frame, p.xPos - 8f, p.yPos - 4f, 32, 32);
            portalRenderer.end();
        }

        // Entity render
        playerEntity.renderEntity(delta / 1000f, entityCamera, !footColliderList.isEmpty(), pauseForActions || gameComplete);

        // Collision debug
        if (RENDER_DEBUG)
            box2DDebugRenderer.render(mapManager.currentWorld, mapCamera.combined);

        actionManager.renderActionState(pauseForActions, stage);

        if (gameComplete)
            actionManager.renderEndGame();
    }

    private void postRender(long delta)
    {
        if (pauseForActions == false && gameComplete == false)
            mapManager.currentWorld.step(delta / 1000f, 6, 2);
    }

    @Override
    public void create()
    {
        init();
    }

    @Override
    public void render()
    {
        preRender(System.currentTimeMillis() - lastPreRender);
        lastPreRender = System.currentTimeMillis();

        ScreenUtils.clear(0, 0, 0, 1);
        renderFrame(System.currentTimeMillis() - lastRender);
        lastRender = System.currentTimeMillis();

        postRender(System.currentTimeMillis() - lastPostRender);
        lastPostRender = System.currentTimeMillis();
    }

    @Override
    public void dispose()
    {
        mapManager.dispose();
        mapRenderer.dispose();
        playerEntity.dispose();
    }

    @Override
    public void beginContact(Contact contact)
    {
        if ((int) contact.getFixtureA().getUserData() == MapManager.CTYPE_FOOTS &&
                (int) contact.getFixtureB().getUserData() == MapManager.CTYPE_COLLISION) // Foots
            footColliderList.add(contact.getFixtureB());
        if ((int) contact.getFixtureB().getUserData() == MapManager.CTYPE_FOOTS &&
                (int) contact.getFixtureA().getUserData() == MapManager.CTYPE_COLLISION)
            footColliderList.add(contact.getFixtureA());

        if ((int) contact.getFixtureA().getUserData() == MapManager.CTYPE_PLAYER &&
                (int) contact.getFixtureB().getUserData() == MapManager.CTYPE_PORTAL)
            nextMap();
        if ((int) contact.getFixtureB().getUserData() == MapManager.CTYPE_PLAYER &&
                (int) contact.getFixtureA().getUserData() == MapManager.CTYPE_PORTAL)
            nextMap();
    }

    @Override
    public void endContact(Contact contact)
    {
        if ((int) contact.getFixtureA().getUserData() == MapManager.CTYPE_FOOTS &&
                (int) contact.getFixtureB().getUserData() == MapManager.CTYPE_COLLISION) // Foots
            footColliderList.remove(contact.getFixtureB());
        if ((int) contact.getFixtureB().getUserData() == MapManager.CTYPE_FOOTS &&
                (int) contact.getFixtureA().getUserData() == MapManager.CTYPE_COLLISION)
            footColliderList.remove(contact.getFixtureA());
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {

    }

    @Override
    public boolean keyDown(int keycode)
    {
        return false;
    }

    @Override
    public boolean keyUp(int keycode)
    {
        return false;
    }

    @Override
    public boolean keyTyped(char character)
    {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        if (button == Input.Buttons.LEFT || pointer == 0)
        {
            if (pauseForActions)
                actionManager.mouseClicked(screenX, screenY, stage);
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY)
    {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY)
    {
        return false;
    }
}
