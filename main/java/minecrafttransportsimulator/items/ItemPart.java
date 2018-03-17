package minecrafttransportsimulator.items;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import net.minecraft.item.Item;

public class ItemPart extends Item{
	public final Class<? extends EntityMultipartChild> partClassToSpawn;
	
	public ItemPart(Class<? extends EntityMultipartChild> partClassToSpawn){
		this.partClassToSpawn = partClassToSpawn;
	}
}