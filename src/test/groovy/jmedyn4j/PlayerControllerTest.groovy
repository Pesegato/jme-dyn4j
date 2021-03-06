package jmedyn4j

import org.dyn4j.collision.Fixture
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.dynamics.World
import org.dyn4j.geometry.Capsule
import org.dyn4j.geometry.Circle
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Rectangle
import org.dyn4j.geometry.Transform

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.AnalogListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Box
import com.jme3.scene.shape.Cylinder
import com.jme3.scene.shape.Sphere
import com.jme3.system.AppSettings
import com.jme3.scene.*;

class PlayerControllerTest extends SimpleApplication {

	static final Float Z_THICKNESS=0.5f
	/* 
	 byzanz-record --duration=5 --x=2080 --y=560 --width=320 --height=350 test.gif
	 */
	public static void main(String... args) {
		PlayerControllerTest main = new PlayerControllerTest()
		//main.setDisplayFps(false)
		main.setDisplayStatView(false)
		main.setLostFocusBehavior(LostFocusBehavior.Disabled)
		main.setShowSettings(false)
		AppSettings settings = new AppSettings(true)
		settings.setResolution(300, 300)
		settings.setVSync(true)
		settings.setTitle("JME-DYN4J-TEST")
		main.setSettings(settings)
		println "starting"
		main.start()

	}
	
	@Override
	public void destroy() {
		super.destroy();
		System.exit(0)
	}
	
	Dyn4JAppState dyn4JAppState

	@Override
	public void simpleInitApp() {
		viewPort.setBackgroundColor(new ColorRGBA(new Float(135/255f),new Float(206/255f),new Float(250/255f), 1f))

		inputManager.setCursorVisible(true)
		flyCam.setEnabled(false);
		flyCam.setMoveSpeed(15f)

		dyn4JAppState = new Dyn4JAppState()
		stateManager.attach(dyn4JAppState)

		createWorld(dyn4JAppState)
		cam.setLocation(new Vector3f(0f, 0f, 35f));
		initKeys()
		initPlayer(new Vector2f(0f, 0f), dyn4JAppState)

		new Timer().schedule({
			/*
			 sleep 2 ; byzanz-record --duration=18 --x=0 --y=60 --width=320 --height=325 /home/finn/src/jme-dyn4j/etc/new.gif
			 */
			//"sh /home/finn/src/jme-dyn4j/etc/mv-srv.sh".execute()
		} as TimerTask, 500)
	}
	
	Dyn4JPlayerControl playerControl
	
	private initPlayer(Vector2f location, Dyn4JAppState dyn4JAppState) {
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", ColorRGBA.Red)
		
		Geometry cylGeom = new Geometry("Cylinder", new Cylinder(20, 50, 0.3f, 1.8f))
		cylGeom.setMaterial(mat)
		
		Quaternion roll = new Quaternion()
		roll.fromAngleAxis(new Float(FastMath.PI/2), Vector3f.UNIT_X );
		cylGeom.setLocalRotation(roll)
		
		Node capsuleNode = new Node()
		capsuleNode.attachChild(cylGeom)
		capsuleNode.setLocalTranslation(location.x, location.y, 0f)
		
		rootNode.attachChild(capsuleNode)
		playerControl = new Dyn4JPlayerControl(0.3, 1.80, 90)
		capsuleNode.addControl(playerControl)
		dyn4JAppState.add(capsuleNode)
	}

	void initKeys() {
		inputManager.addMapping("Left",  new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addListener(actionListener,"Left", "Right", "Jump");
	}
/*
	private AnalogListener analogListener = new AnalogListener() {
		public void onAnalog(String name, float value, float tpf) {
			if (name.equals("Right")) {
				println "right"
			}
		}
	};*/

	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Right")) {
				if (keyPressed) playerControl.moveRight()
				else playerControl.stopMoveRight()
			}
			if (name.equals("Left")) {
				if (keyPressed) playerControl.moveLeft()
				else playerControl.stopMoveLeft()
			}
			
			if (name.equals("Jump")) {
				playerControl.jump()
			}
		}
	};

	void createWorld(Dyn4JAppState dyn4JAppState) {
		createFloor(dyn4JAppState);
		(1..40).each {
			createBox(new Vector2f(new Float((Math.random()*15)-7), new Float(-8)), dyn4JAppState)
		}
	}



	Double cageSize = 10

	private createFloor(Dyn4JAppState dyn4JAppState) {
		Double width = cageSize
		Double thickness = 0.3
		Box b = new Box(new Float(width), new Float(thickness), Z_THICKNESS)
		Geometry floorGeom = new Geometry("Box", b)
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
		mat.getAdditionalRenderState().setWireframe(true)
		mat.setColor("Color", new ColorRGBA(new Float(160/255f),new Float(82/255f),new Float(45/255f), 1f))
		floorGeom.setMaterial(mat)
		floorGeom.setLocalTranslation(0f, -8f, 0f)
		rootNode.attachChild(floorGeom)

		floorGeom.addControl(new Dyn4JShapeControl(new Rectangle(width*2, thickness*2), MassType.INFINITE, 0, 1))
		dyn4JAppState.add(floorGeom)
	}

	private createBox(Vector2f location, Dyn4JAppState dyn4JAppState) {
		Double boxSize = 0.5f;
		Box b = new Box(new Float(boxSize), new Float(boxSize), Z_THICKNESS)
		Geometry boxGeom = new Geometry("Box", b)
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", new ColorRGBA(new Float(255/255f),new Float(228/255f),new Float(225/255f), 1f))

		boxGeom.setLocalTranslation(location.x, location.y, 0f)
		boxGeom.setMaterial(mat)
		rootNode.attachChild(boxGeom)
		Dyn4JShapeControl physics = new Dyn4JShapeControl(new Rectangle(new Float(boxSize*2), new Float(boxSize*2)), MassType.NORMAL, 100, 0.8, 0)
		boxGeom.addControl(physics)
		dyn4JAppState.add(boxGeom)
	}
}