package net.buddat.ludum.ld51;

import com.badlogic.gdx.graphics.Texture;

public abstract class Action
{

    private String name;

    private Texture icon;

    private Entity linkedEntity;

    public Action(Entity entity, String name, String icon)
    {
        this.name = name;
        this.icon = new Texture(icon);
        this.linkedEntity = entity;
    }

    public abstract void trigger();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Texture getIcon()
    {
        return icon;
    }

    public void setIcon(Texture icon)
    {
        this.icon = icon;
    }

    public Entity getLinkedEntity()
    {
        return linkedEntity;
    }

    public void setLinkedEntity(Entity linkedEntity)
    {
        this.linkedEntity = linkedEntity;
    }

}
