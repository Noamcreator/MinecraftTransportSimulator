package minecrafttransportsimulator.entities.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntityEngineCar;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.sounds.AttenuatedSound;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class EntityCar extends EntityMultipartVehicle implements SFXEntity{	
	public boolean hornOn;
	//Note that angle variable should be divided by 10 to get actual angle.
	public short steeringAngle;
	public short steeringCooldown;
	
	public List<EntityWheel> wheels = new ArrayList<EntityWheel>();
	
	//Internal car variables
	private float momentPitch;
	
	private double wheelForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	private double gravitationalTorque;//kg*m^2/ticks^2
	
	private EntityEngineCar engine;
	private AttenuatedSound hornSound;
	
	public EntityCar(World world){
		super(world);
	}
	
	public EntityCar(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	protected void getBasicProperties(){
		momentPitch = (float) (2*currentMass);
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		//Turn on brake/indicator and backup lights if they are activated.
		changeLightStatus(LightTypes.BRAKELIGHT, brakeOn);
		changeLightStatus(LightTypes.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.LEFTTURNLIGHT));
		changeLightStatus(LightTypes.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.RIGHTTURNLIGHT));
		changeLightStatus(LightTypes.BACKUPLIGHT, this.engine != null && this.engine.getCurrentGear() < 0);
	}
	
	@Override
	protected void getForcesAndMotions(){
		if(engine != null){
			wheelForce = engine.getForceOutput();
		}else{
			wheelForce = 0;
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*5.0F*pack.car.dragCoefficient;
		gravitationalForce = currentMass*(9.8/400);
		gravitationalTorque = gravitationalForce*1;
				
		motionX += (headingVec.xCoord*wheelForce - velocityVec.xCoord*dragForce)/currentMass;
		motionZ += (headingVec.zCoord*wheelForce - velocityVec.zCoord*dragForce)/currentMass;
		motionY += (headingVec.yCoord*wheelForce - velocityVec.yCoord*dragForce - gravitationalForce)/currentMass;
		
		motionYaw = 0;
		motionPitch = (float) (((1-Math.abs(headingVec.yCoord))*gravitationalTorque)/momentPitch);
		motionRoll = 0;
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(steeringCooldown==0){
			if(steeringAngle != 0){
				MTS.MTSNet.sendToAll(new SteeringPacket(this.getEntityId(), steeringAngle < 0, (short) 0));
				steeringAngle += steeringAngle < 0 ? 20 : -20;
			}
		}else{
			--steeringCooldown;
		}
	}
	
	
	@Override
	public void addChild(String childUUID, EntityMultipartChild child, boolean newChild){
		super.addChild(childUUID, child, newChild);
		if(child instanceof EntityWheel){
			if(!wheels.contains(child)){
				wheels.add((EntityWheel) child);
			}
		}else if(child instanceof EntityEngineCar){
			engine = (EntityEngineCar) child;
		}
	}
	
	@Override
	public void removeChild(String childUUID, boolean playBreakSound){
		super.removeChild(childUUID, playBreakSound);
		Iterator<EntityWheel> wheelIterator = wheels.iterator();
		while(wheelIterator.hasNext()){
			if(wheelIterator.next().UUID.equals(childUUID)){
				wheelIterator.remove();
				return;
			}
		}
		if(engine != null && engine.UUID.equals(childUUID)){
			engine = null;
		}
	}

	@Override
	public Instruments getBlankInstrument(){
		return Instruments.AIRCRAFT_BLANK;
	}
	
	@Override
	public float getSteerAngle(){
		return -steeringAngle/10F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return new AttenuatedSound<EntityCar>(MTS.MODID + ":" + pack.car.hornSound, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return hornSound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){
		hornSound = (AttenuatedSound) sound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return hornOn && !isDead;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getVolume(){
		return 5.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getPitch(){
		return 1.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){}

    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.steeringAngle=tagCompound.getShort("steeringAngle");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("steeringAngle", this.steeringAngle);
		return tagCompound;
	}
}