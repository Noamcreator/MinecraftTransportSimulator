package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartSeat;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartSeat extends APart{
	public ItemPart activeGun;
	
	public PartSeat(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		this.activeGun = PackParserSystem.getItem(data.getString("activeGunPackID"), data.getString("activeGunSystemName"));
	}
	
	@Override
	public boolean interact(IWrapperPlayer player){
		//See if we can interact with the seats of this vehicle.
		//This can happen if the vehicle is not locked, or we're already inside a locked vehicle.
		if(!vehicle.locked || vehicle.equals(player.getEntityRiding())){
			IWrapperEntity riderForSeat = vehicle.locationRiderMap.get(placementOffset);
			if(riderForSeat != null){
				//We already have a rider for this seat.  If it's not us, mark the seat as taken.
				//If it's an entity that can be leashed, dismount the entity and leash it.
				if(riderForSeat instanceof IWrapperPlayer){
					if(!player.equals(riderForSeat)){
						player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
					}
				}else if(!riderForSeat.leashTo(player)){
					//Can't leash up this entity, so mark the seat as taken.
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
				}
			}else{
				//Seat is free.  Either mount this seat, or if we have a leashed animal, set it in that seat.
				IWrapperEntity leashedEntity = player.getLeashedEntity();
				if(leashedEntity != null){
					vehicle.addRider(leashedEntity, placementOffset);
				}else{
					//Didn't find an animal.  Just mount the player.
					//Don't mount them if they are sneaking, however.  This will confuse MC.
					if(!player.isSneaking()){
						vehicle.addRider(player, placementOffset);
						//If this seat can control a gun, and isn't controlling one, set it now.
						//This prevents the need to select a gun when initially mounting.
						//If we do have an active gun, validate that it's still correct.
						if(activeGun == null){
							setNextActiveGun();
							MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartSeat(this));
						}else{
							for(ItemPart gunType : vehicle.guns.keySet()){
								for(PartGun gun : vehicle.guns.get(gunType)){
									if(player.equals(gun.getCurrentController())){
										if(gunType.equals(activeGun)){
											return true;
										}
									}
								}
							}
							
							//Didn't invalid active gun detected.  Select a new one.
							activeGun = null;
							setNextActiveGun();
							MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartSeat(this));
						}
					}
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
		}
		return true;
    }
	
	/**
	 * Sets the next active gun for this seat.  Active guns are queried by checking guns to
	 * see if this rider can control them.  If so, then the active gun is set to that gun type.
	 */
	public void setNextActiveGun(){
		IWrapperEntity rider = vehicle.locationRiderMap.get(placementOffset);
		//Iterate over all the gun types, attempting to get the type after our selected type.
		//If we don't have an active gun, just get the next possible unit.
		if(activeGun == null){
			for(ItemPart gunType : vehicle.guns.keySet()){
				for(PartGun gun : vehicle.guns.get(gunType)){
					if(rider.equals(gun.getCurrentController())){
						activeGun = gunType;
						return;
					}
				}
			}
		}else{
			ItemPart firstPossibleGun = null;
			ItemPart currentGun = activeGun;
			activeGun = null;
			boolean pastActiveGun = false;
			for(ItemPart gunType : vehicle.guns.keySet()){
				for(PartGun gun : vehicle.guns.get(gunType)){
					if(rider.equals(gun.getCurrentController())){
						if(pastActiveGun){
							activeGun = gunType;
							return;
						}else{
							if(firstPossibleGun == null){
								firstPossibleGun = gunType;
							}
							if(gunType.equals(currentGun)){
								pastActiveGun = true;
							}
						}
						break;
					}
				}
			}
			//If the active gun is null, we just set it to the first possible gun as we've gone around.
			if(activeGun == null){
				activeGun = firstPossibleGun;
			}
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		IWrapperEntity rider = vehicle.locationRiderMap.get(placementOffset);
		if(rider != null){
			vehicle.removeRider(rider, null);
		}
	}
	
	@Override
	public IWrapperNBT getData(){
		IWrapperNBT data = super.getData();
		if(activeGun != null){
			data.setString("activeGunPackID", activeGun.definition.packID);
			data.setString("activeGunSystemName", activeGun.definition.systemName);
		}
		return data;
	}

	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}
}
