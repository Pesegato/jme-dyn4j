package jmedyn4j

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppState
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node
import com.jme3.scene.Spatial;

import javax.sql.rowset.spi.SyncResolver;

import org.dyn4j.dynamics.World

class Dyn4JAppState extends AbstractAppState {
	private World world
	private Set<Spatial> spatials = new HashSet<Spatial>();
	
	void add(Spatial spatial) {
		println "add"
		if (world == null) world = new World()
		if (spatial.getControl(Dyn4JShapeControl.class) == null) throw new IllegalArgumentException("Cannot handle a node which isnt a ${Dyn4JShapeControl.getClass().getSimpleName()}")
		synchronized(spatials) {
			 spatials.add(spatial)
			 Dyn4JShapeControl ctl = spatial.getControl(Dyn4JShapeControl.class)
			 world.addBody(ctl.body)
		}
	}
	
	@Override
	public void stateAttached(AppStateManager stateManager) {
		super.stateAttached(stateManager)
	}

	@Override
	public void stateDetached(AppStateManager stateManager) {
		super.stateDetached(stateManager);
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf)
		println "world update"
		world.update(tpf)
		synchronized(spatials) {
			spatials.asList().each { Spatial spatial ->
				Dyn4JShapeControl ctl = spatial.getControl(Dyn4JShapeControl.class)
				if (ctl == null) { spatials.remove(spatial); return; } //evict nodes which have their Dyn4JShapeControl removed
				
				ctl.updateFromAppState()
			}
		}
	}

	
	


	@Override
	public void initialize(AppStateManager stateManager, Application app) {
	  super.initialize(stateManager, app);
	  
	}

}
