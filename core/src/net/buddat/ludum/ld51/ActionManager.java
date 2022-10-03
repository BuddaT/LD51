package net.buddat.ludum.ld51;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.HashMap;

public class ActionManager
{

    public float totalTimeTaken;

    private float thisLevelTaken;

    private float lastLevelTaken;

    private float currentTime;

    private ShapeRenderer shapeRenderer;

    private SpriteBatch iconRenderer;

    private BitmapFont fontRenderer;

    private static GlyphLayout glyphLayout = new GlyphLayout();

    private UIShape timeTracker;

    private ArrayList<Action> triggeredActions = new ArrayList<>();

    private HashMap<Action, Float> actionTimingMap = new HashMap<>();

    private Action selectedAction = null;

    public void init()
    {
        shapeRenderer = new ShapeRenderer();
        iconRenderer = new SpriteBatch();
        timeTracker = new UIShape(10, Gdx.graphics.getHeight() - 50, Gdx.graphics.getWidth() - 20,  40);
        fontRenderer = new BitmapFont();
    }

    public void setAction(Action action, float offset)
    {
        actionTimingMap.put(action, offset);
    }

    public void resetTime(boolean resetLevel, boolean nextMap)
    {
        currentTime = 0f;
        triggeredActions.clear();
        if (nextMap)
            lastLevelTaken = thisLevelTaken;
        if (resetLevel)
            thisLevelTaken = 0f;
    }

    public void pollActions(float delta)
    {
        currentTime += delta;
        totalTimeTaken += delta;
        thisLevelTaken += delta;

        for (Action a : actionTimingMap.keySet())
        {
            if (triggeredActions.contains(a))
                continue;

            if (actionTimingMap.get(a) < currentTime)
            {
                if (actionTimingMap.get(a) > 0f)
                    a.trigger();
                triggeredActions.add(a);
            }
        }

        if (currentTime >= 10f)
        {
            resetTime(false, false);
        }
    }

    public void renderActionState(boolean choosingActions, int mapNumber)
    {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapeRenderer.setColor(Color.LIGHT_GRAY);
            shapeRenderer.rect(timeTracker.xPos, timeTracker.yPos, timeTracker.width, timeTracker.height);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        {
            float xLine = timeTracker.xPos + (timeTracker.getWidth() * (currentTime / 10f));
            shapeRenderer.setColor(Color.valueOf("#007722"));
            shapeRenderer.line(xLine, timeTracker.yPos + 2, xLine, timeTracker.yPos + timeTracker.getHeight() - 2);

            shapeRenderer.setColor(Color.DARK_GRAY);
            shapeRenderer.rect(timeTracker.xPos, timeTracker.yPos, timeTracker.width, timeTracker.height);

            for (int i = 1; i < 10; i++)
            {
                xLine = timeTracker.xPos + (timeTracker.getWidth() * (i / 10f));
                shapeRenderer.line(xLine, timeTracker.yPos, xLine, timeTracker.yPos + 5);
                shapeRenderer.line(xLine, timeTracker.yPos + timeTracker.getHeight() - 5, xLine,
                        timeTracker.yPos + timeTracker.getHeight());
            }
        }
        shapeRenderer.end();

        iconRenderer.begin();
        for (Action a : actionTimingMap.keySet())
        {
            float xIcon = timeTracker.xPos + (timeTracker.getWidth() * (actionTimingMap.get(a) / 10f));
            iconRenderer.draw(a.getIcon(), 10 + xIcon - a.getIcon().getWidth() / 2f, timeTracker.yPos + 4);
        }
        iconRenderer.end();

        iconRenderer.begin();
        {
            fontRenderer.setColor(Color.BLACK);
            fontRenderer.getData().setScale(1.3f);
            fontRenderer.draw(iconRenderer, "[R]estart", Gdx.graphics.getWidth() - 87,  Gdx.graphics.getHeight() - 22);

            fontRenderer.setColor(Color.WHITE);
            fontRenderer.getData().setScale(2f);
            int seconds = (int) (thisLevelTaken);
            int ms = (int) ((thisLevelTaken * 100) % 100);
            int minutes = (int) seconds / 60;
            if (minutes > 0)
                seconds = seconds % 60;
            fontRenderer.draw(iconRenderer, (minutes < 10 ?
             "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "" ) + seconds + ":" + (ms < 10 ? "0" : "") + ms,
                    Gdx.graphics.getWidth() - 150, 75);

            fontRenderer.setColor(Color.LIGHT_GRAY);
            fontRenderer.getData().setScale(1.5f);
            seconds = (int) (totalTimeTaken);
            ms = (int) ((totalTimeTaken * 100) % 100);
            minutes = (int) seconds / 60;
            if (minutes > 0)
                seconds = seconds % 60;
            fontRenderer.draw(iconRenderer, (minutes < 10 ?
                            "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "" ) + seconds + ":" + (ms < 10 ? "0" : "") + ms,
                    Gdx.graphics.getWidth() - 123, 45);
        }
        iconRenderer.end();

        if (choosingActions)
        {
            // UI to select and put actions down in the top bar
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            {
                int total = 0;
                for (Action a : actionTimingMap.keySet())
                {
                    if (actionTimingMap.get(a) < 0 && actionTimingMap.get(a) < -(mapNumber))
                        continue;
                    if (actionTimingMap.get(a) < 0)
                        actionTimingMap.put(a, (float) (Math.random() * 10f));
                    total++;
                }
                shapeRenderer.setColor(Color.LIGHT_GRAY);
                shapeRenderer.rect(Gdx.graphics.getWidth() - (100 * total) - 10, Gdx.graphics.getHeight() - 180,
                        64 * total + (36 * (total - 1)) + 20, 100);
            }
            shapeRenderer.end();

            iconRenderer.begin();
            {
                int seconds = (int) (lastLevelTaken);
                int ms = (int) ((lastLevelTaken * 100) % 100);
                int minutes = (int) seconds / 60;
                if (minutes > 0)
                    seconds = seconds % 60;

                fontRenderer.setColor(Color.LIGHT_GRAY);
                fontRenderer.getData().setScale(2f);
                fontRenderer.draw(iconRenderer, "Last Level Time: " + (minutes < 10 ?
                        "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "" ) + seconds + ":" + (ms < 10 ? "0" : "") + ms, 12,
                        180);

                fontRenderer.setColor(Color.WHITE);
                fontRenderer.getData().setScale(2.3f);
                fontRenderer.draw(iconRenderer, "Click on an action above, then click on the timeline to place it", 10, 120);

                fontRenderer.setColor(Color.LIGHT_GRAY);
                fontRenderer.getData().setScale(4f);
                fontRenderer.draw(iconRenderer, "Press ENTER to Continue", 12, 75);

                int count = 1;
                for (Action a : actionTimingMap.keySet())
                {
                    if (actionTimingMap.get(a) < 0 && actionTimingMap.get(a) < -(mapNumber))
                        continue;

                    float xIcon = Gdx.graphics.getWidth() - (100 * count++);
                    iconRenderer.draw(a.getIcon(), xIcon, Gdx.graphics.getHeight() - 150, 64, 64);

                    fontRenderer.setColor(Color.DARK_GRAY);
                    fontRenderer.getData().setScale(1f);
                    glyphLayout.setText(fontRenderer, a.getName());
                    fontRenderer.draw(iconRenderer, a.getName(), xIcon + a.getIcon().getWidth() - glyphLayout.width / 2f,
                            Gdx.graphics.getHeight() - 160);

                    if (selectedAction == a)
                    {
                        iconRenderer.end();
                        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                        {
                            shapeRenderer.setColor(Color.valueOf("#007722"));
                            shapeRenderer.rect(xIcon, Gdx.graphics.getHeight() - 150, 64, 64);
                        }
                        shapeRenderer.end();
                        iconRenderer.begin();
                    }
                }
            }
            iconRenderer.end();
        }
    }

    public void renderEndGame()
    {
        iconRenderer.begin();
        {
            fontRenderer.setColor(Color.valueOf("#22AA44"));
            fontRenderer.getData().setScale(4f);
            fontRenderer.draw(iconRenderer, "Game Completed!", 250, 300);

            int seconds = (int) (totalTimeTaken);
            int ms = (int) ((totalTimeTaken * 100) % 100);
            int minutes = (int) seconds / 60;
            if (minutes > 0)
                seconds = seconds % 60;

            fontRenderer.setColor(Color.WHITE);
            fontRenderer.getData().setScale(2f);
            fontRenderer.draw(iconRenderer, "Total Time: " + (minutes < 10 ?
                    "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "" ) + seconds + ":" + (ms < 10 ? "0" : "") + ms, 355, 230);
            fontRenderer.setColor(Color.valueOf("#66DD88"));
            fontRenderer.draw(iconRenderer, "Press [R] to play again with all actions!", 250, 170);

            fontRenderer.setColor(Color.valueOf("#AABBAA"));
            fontRenderer.draw(iconRenderer, "Thanks for playing!", 365, 50);
        }
        iconRenderer.end();
    }

    public void mouseClicked(int screenX, int screenY, int mapNumber)
    {
        if (selectedAction != null && screenY < 50 && screenY > 10 && screenX > 10 && screenX < Gdx.graphics.getWidth() - 10)
        {
            float xScaled = (screenX - 20) / (float) (Gdx.graphics.getWidth() - 20);
            actionTimingMap.put(selectedAction, xScaled * 10f);
        }
        else
        {
            int count = 1;
            selectedAction = null;
            for (Action a : actionTimingMap.keySet())
            {
                if (actionTimingMap.get(a) < 0 && actionTimingMap.get(a) < -(mapNumber))
                    continue;

                float xIcon = Gdx.graphics.getWidth() - (100 * count++);
                float width = 64;

                if (screenX < xIcon || screenX > xIcon + width)
                    continue;

                float yIcon = 150 - 64;
                float height = 64;

                if (screenY < yIcon || screenY > yIcon + height)
                    continue;

                selectedAction = a;
                break;
            }
        }
    }

    public class UIShape
    {
        private float xPos, yPos;

        private float width, height;

        public UIShape(float xPos, float yPos, float width, float height)
        {
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = width;
            this.height = height;
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

        public float getWidth()
        {
            return width;
        }

        public void setWidth(float width)
        {
            this.width = width;
        }

        public float getHeight()
        {
            return height;
        }

        public void setHeight(float height)
        {
            this.height = height;
        }
    }
}
