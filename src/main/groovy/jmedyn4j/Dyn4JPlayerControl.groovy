package jmedyn4j

import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.dynamics.Force
import org.dyn4j.dynamics.World
import org.dyn4j.dynamics.joint.Joint
import org.dyn4j.dynamics.joint.MotorJoint
import org.dyn4j.geometry.AbstractShape
import org.dyn4j.geometry.Capsule
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Transform
import org.dyn4j.geometry.Vector2

import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Spatial
import com.jme3.scene.control.Control

class Dyn4JPlayerControl implements Control, IDyn4JControl {
	private final Double walkSpeed
	private final Double jumpForceFactor
	private Double weight

	/* When correction is recieved: */
	private final static Double rubberBandThreshold = 0.8;
	private final static Double neglishableCorrectionThreshold = 0.1;
	private final static Double neglishableCorrectionPercentage = 10
	/* End Correction */	
	
	/* The body. */
	private Spatial spatial
	private final Body mainBody
	private final Body controllerbody
	private final MotorJoint joint
	
	/* Movement state */
	private Boolean walkRight = false
	private Boolean walkLeft = false
	private Boolean jump = false
	
	Dyn4JPlayerControl(Double width=0.3, Double height=1.8, Long weight=80, Double walkSpeed=4, Double jumpForceFactor=3.0, Double friction=1, Double resitution = 0) {
		this.weight = weight;
		this.walkSpeed = walkSpeed
		this.jumpForceFactor = jumpForceFactor
		
		AbstractShape shape = new Capsule(width, height)
		mainBody = new Body()
		BodyFixture bodyFixture = new BodyFixture(shape)
		bodyFixture.setDensity(weight);
		bodyFixture.setFriction(friction) //1=no slipperiness
		bodyFixture.setRestitution(resitution) //0=no bouncyness
		mainBody.addFixture(bodyFixture)
		mainBody.setMass(MassType.NORMAL)
		mainBody.setAutoSleepingEnabled(false)
		
		controllerbody = new Body()
		BodyFixture controllerFixture = new BodyFixture(shape)
		controllerFixture.setSensor(true)
		controllerFixture.setDensity(weight);
		controllerFixture.setFriction(friction) //1=no slipperiness
		controllerFixture.setRestitution(resitution) //1=no slipperiness
		controllerbody.addFixture(controllerFixture);
		controllerbody.setMass(MassType.INFINITE)
		
		joint = new MotorJoint(controllerbody, mainBody);
		joint.setLinearTarget(new Vector2(0.5, 0.5));
		joint.setAngularTarget(Math.PI * 1.0);
		joint.setCorrectionFactor(1.0);
		joint.setMaximumForce(1);
		joint.setMaximumTorque(Double.MAX_VALUE);
		joint.setCollisionAllowed(false)
	}
	
	@Override 
	void addToWorld(World world) {
		world.addBody(mainBody)
		world.addBody(controllerbody)
		world.addJoint(joint)
	}
	
	@Override
	public void setSpatial(Spatial spatial) {
		this.spatial = spatial
		mainBody.translate(new Double(spatial.getLocalTranslation().x), new Double(spatial.getLocalTranslation().y))
		controllerbody.translate(new Double(spatial.getLocalTranslation().x), new Double(spatial.getLocalTranslation().y))
	}

	private Double lastAngle=-1
	private Transform lastTransform = new Transform()
	
	void updatePhysics(float tpf) {
		updateWalkDirection(tpf)
		updateJump(tpf)
	}
	
	void updateDraw(float tpf) {
		updateLocation()
		updateRotation()
	}

	private updateRotation() {
		Double angle = mainBody.getTransform().getRotation()
		if (angle != lastAngle) {
			Quaternion roll = new Quaternion()
			roll.fromAngleAxis( new Float(angle) , Vector3f.UNIT_Z);
			this.spatial.setLocalRotation(roll)
			lastAngle = angle
		}
	}

	Map getTrlv() {
		Vector2 tr = mainBody.getTransform().getTranslation()
		Vector2 lv = mainBody.getLinearVelocity()
		return [tr:[x:tr.x.round(4), y:tr.y.round(4)], lv:[x:lv.x.round(4), y:lv.y.round(4)]]
	}

	void performCorrection(Map newTrlv) {
		//println "PerformCorrection"
		
		Double xDiff = mainBody.getTransform().getTranslationX() - newTrlv.tr.x 
		Double yDiff = mainBody.getTransform().getTranslationY() - newTrlv.tr.y
		
		if (Math.abs(xDiff) > rubberBandThreshold) {
			println "X rubber band"
			Double newTrX = newTrlv.tr.x
			mainBody.getTransform().setTranslationX(newTrX)
		} else if (Math.abs(xDiff) > neglishableCorrectionThreshold){
			//println "X 10% corr"
		 	Double newTrX = mainBody.getTransform().getTranslationX() + (xDiff / neglishableCorrectionPercentage)
			mainBody.getTransform().setTranslationX(newTrX)
		} else {
			//println "X too simular, ignored"
		}
		
		if (Math.abs(yDiff) > rubberBandThreshold) {
			println "Y rubber band"
			Double newTrY = newTrlv.tr.y
			mainBody.getTransform().setTranslationY(newTrY)
		} else if (Math.abs(yDiff) > neglishableCorrectionThreshold){
			//println "Y 10% corr"
		 	Double newTrY = mainBody.getTransform().getTranslationY() + (yDiff / neglishableCorrectionPercentage)
			mainBody.getTransform().setTranslationY(newTrY)
		} else {
			//println "Y too simular, ignored"
		}
	}
	
	void setTrlv(Map trlv) {
		Transform tr = new Transform()
		tr.setTranslation(trlv.tr.x, trlv.tr.y)
		tr.setRotation(mainBody.getTransform().getRotation())
		mainBody.setTransform(tr)
		mainBody.setLinearVelocity(trlv.lv.x, trlv.lv.y)
	}
	
	private updateLocation() {
		Vector2 vector2 = mainBody.getTransform().getTranslation()
		this.spatial.setLocalTranslation(
				new Float(vector2.x),
				new Float(vector2.y), 0f)
	}
		
	private updateJump(float tpf) {
		if (jump) {
			jump=false
			mainBody.applyImpulse(new Vector2(0, weight*jumpForceFactor));
		}
	}
	
	Boolean canJump() {
		(mainBody.getInContactBodies(false).size() != 0)
	}
	
	private void updateWalkDirection(float tpf) {
		if (walkRight) {
			mainBody.setLinearVelocity(walkSpeed, mainBody.getLinearVelocity().y) //hard turn
		} else if (walkLeft) {
			mainBody.setLinearVelocity(-walkSpeed, mainBody.getLinearVelocity().y)//hard turn
		} else { //stop, let physics (friction etc) take care of it.
		}
	}
	
	void doMove(String move) {
		switch (move) {
			case ("Join"):  break; /*ignore*/
			case ("Right"): moveRight(); break;
			case ("Left"): moveLeft(); break;
			case ("StopLeft"): stopMoveLeft(); break;
			case ("StopRight"): stopMoveRight(); break;
			case ("Jump"): jump(); break;
			default: println "No idea what to do about $move"; 
		}
	}
	
	void setMove(String move) {
		switch (move) {
			case ("Join"):  break; /*ignore*/
			case ("NoMove"): walkRight=false;walkLeft=false; break;
			case ("Right"): walkRight=true; break;
			case ("Left"): walkLeft=true; break;
			case ("StopLeft"): walkLeft=false; break;
			case ("StopRight"): walkRight=false; break;
			case ("Jump"): jump=true;break;
			default: println "No idea what to do about $move";
		}
	}
	
	String getMove() {
		if (walkRight) return "Right"
		if (walkLeft) return "Left"
		if (jump) return "Jump"
		return "Nothing"
	}
	
	void moveRight() {
		walkRight=true
	}
	
	void moveLeft() {
		walkLeft=true
	}
	
	void stopMoveRight() {
		walkRight=false
	}
	
	void stopMoveLeft() {
		walkLeft=false
	}
	
	void jump() {
		jump=true
	}
	
	@Override
	public void update(float tpf) {
	}
	
	@Override 
	public void write(JmeExporter ex) throws IOException {}

	@Override 
	public void read(JmeImporter im) throws IOException {}

	@Override
	public Control cloneForSpatial(Spatial spatial) {
		return null;
	}
	@Override
	public void render(RenderManager rm, ViewPort vp) {
	}
}
