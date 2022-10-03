package net.buddat.ludum.ld51;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.ArrayList;
import java.util.Iterator;

public class MapManager
{

    private static final String COLLIDER_MAPLAYER = "colliders";

    private static final String PORTAL_MAPLAYER = "portals";

    public static final int CTYPE_PLAYER = 1;
    public static final int CTYPE_FOOTS = 2;
    public static final int CTYPE_COLLISION = 3;
    public static final int CTYPE_PORTAL = 4;

    public String currentMapFile;

    public String nextMapFile;

    public TiledMap currentMap;

    public TiledMap nextMap;

    public int startX;

    public int startY;

    public World currentWorld;

    public World nextWorld;

    private TmxMapLoader mapLoader = null;

    private ArrayList<PortalInfo> nextPortalList = new ArrayList<>();

    private ArrayList<PortalInfo> portalList = new ArrayList<>();

    public void reloadCurrentMap()
    {
        if (currentMapFile != null)
        {
            System.out.println("Loading: " + currentMapFile);
            loadTiledMap(currentMapFile, 0, -10);
            changeToNextMap();
        }
    }

    public void loadTiledMap(String fileName, int gravityX, int gravityY)
    {
        if (mapLoader == null)
            mapLoader = new TmxMapLoader();

        nextMapFile = fileName;
        nextMap = mapLoader.load(nextMapFile);

        nextWorld = new World(new Vector2(gravityX, gravityY), true);

        MapLayer colliderObjects = nextMap.getLayers().get(COLLIDER_MAPLAYER);
        Iterator it = colliderObjects.getObjects().iterator();
        while (it.hasNext())
        {
            MapObject obj = (MapObject) it.next();
            MapProperties props = obj.getProperties();

            float xPos = props.get("x", 0f, Float.class);
            float yPos = props.get("y", 0f, Float.class);
            float width = props.get("width", 100f, Float.class);
            float height = props.get("height", 100f, Float.class);
            float density = props.get("density", 1f, Float.class);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(width / 16f, height / 16f);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = density;

            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.StaticBody;
            bodyDef.position.set(xPos / 8f + (width / 16f), yPos / 8f + (height / 16f));

            Body body = nextWorld.createBody(bodyDef);
            Fixture fixture = body.createFixture(fixtureDef);
            fixture.setUserData(CTYPE_COLLISION);

            shape.dispose();
        }

        nextPortalList.clear();
        MapLayer portalObjects = nextMap.getLayers().get(PORTAL_MAPLAYER);
        if (portalObjects != null)
        {
            it = portalObjects.getObjects().iterator();
            while (it.hasNext())
            {
                MapObject obj = (MapObject) it.next();
                MapProperties props = obj.getProperties();

                float xPos = props.get("x", 0f, Float.class);
                float yPos = props.get("y", 0f, Float.class);
                float width = props.get("width", 100f, Float.class);
                float height = props.get("height", 100f, Float.class);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(width / 16f, height / 16f);

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;
                fixtureDef.isSensor = true;

                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.position.set(xPos / 8f + (width / 16f), yPos / 8f + (height / 16f));

                Body body = nextWorld.createBody(bodyDef);
                Fixture fixture = body.createFixture(fixtureDef);
                fixture.setUserData(CTYPE_PORTAL);

                shape.dispose();

                nextPortalList.add(new PortalInfo(xPos, yPos));
            }
        }

        System.out.println("Loaded map: " + nextMapFile);

        if (currentMap == null)
        {
            currentMap = nextMap;
            nextMap = null;
            currentWorld = nextWorld;
            nextWorld = null;
            currentMapFile = nextMapFile;
            nextMapFile = null;
            portalList.clear();
            portalList.addAll(nextPortalList);
            nextPortalList.clear();

            startX = currentMap.getProperties().get("startx", 8, Integer.class);
            startY = currentMap.getProperties().get("starty", 10, Integer.class);

            String toLoad = currentMap.getProperties().get("nextMap", "null", String.class);
            if (!"null".equals(toLoad))
            {
                loadTiledMap(toLoad, 0, -10);
            }
        }
    }

    public ArrayList<PortalInfo> getPortalList()
    {
        return portalList;
    }

    public Body addDynamicEntity(Shape s, float density, float bounciness, float xPos, float yPos, boolean hasFoots)
    {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = s;
        fixtureDef.density = density;
        fixtureDef.restitution = bounciness;
        fixtureDef.friction = 0f;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(xPos, yPos);

        Body toReturn = currentWorld.createBody(bodyDef);
        Fixture fixture = toReturn.createFixture(fixtureDef);
        fixture.setUserData(CTYPE_PLAYER);

        if (hasFoots)
        {
            PolygonShape foots = new PolygonShape();
            foots.setAsBox(0.3f, 0.2f, new Vector2(0f, -0.67f), 0f);
            FixtureDef footsDef = new FixtureDef();
            footsDef.isSensor = true;
            footsDef.shape = foots;
            Fixture footsFixture = toReturn.createFixture(footsDef);
            footsFixture.setUserData(CTYPE_FOOTS);

            foots.dispose();
        }

        s.dispose();

        return toReturn;
    }

    public void changeToNextMap()
    {
        World thisWorld = currentWorld;
        TiledMap thisMap = currentMap;

        currentWorld = nextWorld;
        nextWorld = null;
        currentMap = nextMap;
        nextMap = null;
        currentMapFile = nextMapFile;
        nextMapFile = null;
        portalList.clear();
        portalList.addAll(nextPortalList);
        nextPortalList.clear();

        startX = currentMap.getProperties().get("startx", 8, Integer.class);
        startY = currentMap.getProperties().get("starty", 10, Integer.class);

        thisWorld.dispose();
        thisMap.dispose();

        String toLoad = currentMap.getProperties().get("nextMap", "null", String.class);
        if (!"null".equals(toLoad))
        {
            loadTiledMap(toLoad, 0, -10);
        }
    }

    public void dispose()
    {
        currentMap.dispose();
        currentWorld.dispose();
    }

    public class PortalInfo
    {
        float xPos, yPos;

        public PortalInfo(float xPos, float yPos)
        {
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public float getxPos()
        {
            return xPos;
        }

        public void setxPos(float xPos)
        {
            this.xPos = xPos;
        }

        public float getyPos()
        {
            return yPos;
        }

        public void setyPos(float yPos)
        {
            this.yPos = yPos;
        }
    }
}
