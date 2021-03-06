package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component{
	
	private final List<String> textLines = new ArrayList<String>();
	
	public TileEntityPole_Sign(ItemPoleComponent item){
		super(item);
		//Populate the textLines with blank strings at construction.
		if(definition.general.textObjects != null){
			for(byte i=0; i<definition.general.textObjects.size(); ++i){
				textLines.add("");
			}
		}
	}
	
	@Override
	public float lightLevel(){
		return 0;
	}
	
	
	@Override
	public List<String> getTextLines(){
		return textLines;
	}
	
	@Override
	public void setTextLines(List<String> textLines){
		if(textLines != null){
			this.textLines.clear();
			this.textLines.addAll(textLines);
		}
	}
}
