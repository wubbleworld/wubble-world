package edu.isi.wubble.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

import org.ibex.nestedvm.Runtime.CallException;
import org.ibex.nestedvm.Runtime.FaultException;
import org.ibex.nestedvm.Runtime.ReadFaultException;

import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.scene.TriMesh;
import com.jme.scene.batch.GeomBatch;

import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.PhysicsEntity;

public class Swift {
	
	private static Swift swift = null;
	
	public static final int INT_SIZE = 4;
	public static final int FLOAT_SIZE = 4;
	public static final int DOUBLE_SIZE = 8;
	
	private HashMap<String,Integer> _idMap;
	private HashMap<Integer,Entity>  _entityMap;

	private org.ibex.nestedvm.Runtime _runtime;
	
	private Swift() {
		_idMap = new HashMap<String,Integer>();
		_entityMap = new HashMap<Integer,Entity>();
		
		_runtime = new edu.isi.wubble.swift.Main();
		_runtime.start();
		
		if (_runtime.execute()) {
			System.err.println("Exited : " + _runtime.exitStatus());
			return;
		} 
		
		System.out.println("SWIFT has been initialized.");
	}
	
	public static Swift inst() {
		if (swift == null) {
			swift = new Swift();
		}
		return swift;
	}
	
	public void cleanup() {
		_runtime.execute();
	}
	
	public void removeObject(Entity e) {
		if (_idMap.containsKey(e.getName())) {
			int id = _idMap.get(e.getName()).intValue();
			
			try {
				_idMap.remove(e.getName());
				_entityMap.remove(id);
				_runtime.call("deleteObject", new Object[] { id });
			} catch (FaultException e1) {
				e1.printStackTrace();
			} catch (CallException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * add an object to the SWIFT library so that we can 
	 * track distances between it and all the other objects
	 * in the world.  Fixed lets us optimize those entities
	 * that will not be moving.
	 * @param e
	 * @param fixed
	 * @throws Exception
	 */
	public void addObject(Entity e) {
//		System.out.println("Adding: " + e.getName());
		try {
			int nv = 0;
			int nf = 0;
			boolean useSize = false;
			int fixedVal = 0;
			if ("static".equals(e.getEntityType()))
				fixedVal = 1; 
			else
				useSize = true;
			
			Vector3f size = e.getSize();
			
			ByteArrayOutputStream v = new ByteArrayOutputStream();
			DataOutputStream V = new DataOutputStream(v);
			
			ByteArrayOutputStream f = new ByteArrayOutputStream();
			DataOutputStream F = new DataOutputStream(f);

			for (TriMesh t : e.getMeshes()) {
				nv += t.getVertexCount();
				
				int batchCount = t.getBatchCount();
				if (batchCount > 1) {
					System.out.println("object: " + e.getName() + " has more than one batch of vertices");
				}

//				System.out.println("Vertices:");
				GeomBatch b = t.getBatch(0);
				FloatBuffer buf = b.getVertexBuffer();
				for (int i = 0; i < buf.capacity(); ++i) {
//					if (i%3 == 0)
//						System.out.println("...v: " + buf.get(i) + " " + buf.get(i+1) + " " + buf.get(i+2));
					float vi = buf.get(i);
					if (useSize) {
						switch (i%3) {
							case 0:	
								vi *= size.x;
								break;
							case 1:
								vi *= size.y;
								break;
							case 2:
								vi *= size.z;
								break;
						}
					}
					V.writeDouble(new Double(vi));
				}
				
//				System.out.println("Triangles:");
				int triCount = t.getTriangleCount();
				nf += triCount;
				int[] indices = new int[3];
				for (int i = 0; i < triCount; ++i) {
					t.getTriangle(i, indices);
					
//					System.out.println("..tri: " + indices[0] + " " + indices[1] + " " + indices[2]);
					F.writeInt(indices[0]);
					F.writeInt(indices[1]);
					F.writeInt(indices[2]);
				}
//				System.out.println("[" + e.getName() + "] Vertex Count: " + nv + " Face Count: " + nf);
				
			}
			
			int newId = _runtime.call("addObject", new Object[] { nv, v.toByteArray(), nf, f.toByteArray(), fixedVal });
			
			if (fixedVal == 1) {
				update(newId, new Matrix3f(), new Vector3f());
			}
			_idMap.put(e.getName(), newId);
			_entityMap.put(newId, e);
		} catch (CallException exc) {
			exc.printStackTrace();
		} catch (FaultException exc) {
			exc.printStackTrace();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}
	
	/**
	 * update will update the rotation matrix as well as the translation
	 * vector stored within SWIFT.  Only call this when the entity's 
	 * values are actually changing, otherwise you are just wasting time.
	 * @param e
	 */
	public void update(PhysicsEntity e) {
		Integer value = _idMap.get(e.getName());
		if (value == null) 
			return;
		
//		if (e.getName().equals("")) {
//			System.out.println("SWIFT_Update: " + e.getPosition());
//		}
		update(value.intValue(), e.getRotation().toRotationMatrix(), e.getPosition());
	}
	
	private void update(int id, Matrix3f m, Vector3f v) {
		ByteArrayOutputStream r = new ByteArrayOutputStream();
		ByteArrayOutputStream t = new ByteArrayOutputStream();
		
		DataOutputStream R = new DataOutputStream(r);
		DataOutputStream T = new DataOutputStream(t);
		try {
			R.writeDouble(m.m00); R.writeDouble(m.m01); R.writeDouble(m.m02);
			R.writeDouble(m.m10); R.writeDouble(m.m11); R.writeDouble(m.m12);
			R.writeDouble(m.m20); R.writeDouble(m.m21); R.writeDouble(m.m22);
			
			T.writeDouble(v.x); T.writeDouble(v.y); T.writeDouble(v.z);
		} catch (Exception exc) {
			exc.printStackTrace();
			return;
		}
		
		try {
			_runtime.call("transformObject", new Object[] { id, r.toByteArray(), t.toByteArray() });
			_runtime.call("activateObject", new int[] { id });
		} catch (CallException exc) {
			exc.printStackTrace();
		} catch (FaultException exc) {
			exc.printStackTrace();
		}	
	}
	
	/**
	 * this is called before anyone updates so that we can clear
	 * out and deactivate all of the entities that exist in
	 * SWIFT
	 */
	public void preUpdate() {
		try {
			_runtime.call("deactiveAllObjects");
		} catch (CallException exc) {
			exc.printStackTrace();
		} 
	}
	
	/**
	 * update on the whole will actually determine the distances
	 * for each of the objects that are active.  
	 */
	public void update() {
		try {
			_runtime.call("determineDistances");
		
			int np = _runtime.call("getNumPairs");
			if (np == 0) {
//				System.out.println("SWIFT: No One is Moving");
				return;
			}
			
			int pIds = _runtime.call("getIds");
			int pDists = _runtime.call("getDistances");
			int pPoints = _runtime.call("getNearestPoints");
			int pNormals = _runtime.call("getNormals");
			
			byte[] ids = new byte[2*np*INT_SIZE];
			_runtime.copyin(pIds, ids, np*2*INT_SIZE);

			byte[] dists = new byte[np*DOUBLE_SIZE];
			_runtime.copyin(pDists, dists, np*DOUBLE_SIZE);

			byte[] points = new byte[np*DOUBLE_SIZE*6];
			_runtime.copyin(pPoints, points, np*DOUBLE_SIZE*6);
			
			byte[] normals = new byte[np*DOUBLE_SIZE*3];
			_runtime.copyin(pNormals, normals, np*DOUBLE_SIZE*3);
			
			DataInputStream idsDis = new DataInputStream(new ByteArrayInputStream(ids));
			DataInputStream distDis = new DataInputStream(new ByteArrayInputStream(dists));
			DataInputStream pointsDis = new DataInputStream(new ByteArrayInputStream(points));
			DataInputStream normalsDis = new DataInputStream(new ByteArrayInputStream(normals));
			for (int i = 0; i < np; ++i) {
				Entity a = _entityMap.get(idsDis.readInt());
				Entity b = _entityMap.get(idsDis.readInt());
				
				SWIFTContactInfo info = a.getSWIFT(b);
				if (info == null) {
					info = new SWIFTContactInfo(a, b);
					a.setSWIFT(b, info);
					b.setSWIFT(a, info);
				}
					
				float distance = (float) distDis.readDouble();
				distance = (distance < 0) ? 0 : distance;
				info.setDistance(distance);
				
				info.setNearestPoint(a, pointsDis.readDouble(), pointsDis.readDouble(), pointsDis.readDouble());
				info.setNearestPoint(b, pointsDis.readDouble(), pointsDis.readDouble(), pointsDis.readDouble());
				
				info.setNormal(normalsDis.readDouble(), normalsDis.readDouble(), normalsDis.readDouble());
			}
		} catch (ReadFaultException e) {
			e.printStackTrace();
		} catch (CallException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}