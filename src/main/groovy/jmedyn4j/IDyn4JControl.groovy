package jmedyn4j

import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.World
import org.dyn4j.dynamics.joint.Joint

interface IDyn4JControl {
	void updatePhysics(float tpf)
	void updateDraw(float tpf)
	void addToWorld(World world)
	void removeFromWorld(World world)
}
