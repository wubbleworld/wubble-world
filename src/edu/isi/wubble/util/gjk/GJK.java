package edu.isi.wubble.util.gjk;

import java.nio.FloatBuffer;

import com.jme.math.Vector3f;
import com.jme.scene.TriMesh;

import edu.isi.wubble.physics.entity.Entity;

public class GJK {
	
	protected static int DIM = 3;
	protected static int TWO_TO_DIM = 8;
	protected static int DIM_PLUS_ONE = 9;
	protected static int TWICE_TWO_TO_DIM = 16;
	
	protected static int[] _cardinality = new int[] { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4 }; 
	protected static int[] _maxElts = new int[] { -1, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3 };
	
	protected static int[][] _elements = new int[][] {
			{ 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 1, 0, 0, 0 }, { 0, 1, 0, 0 },
			{ 2, 0, 0, 0 }, { 0, 2, 0, 0 }, { 1, 2, 0, 0 }, { 0, 1, 2, 0 },
			{ 3, 0, 0, 0 }, { 0, 3, 0, 0 }, { 1, 3, 0, 0 }, { 0, 1, 3, 0 },
			{ 2, 3, 0, 0 }, { 0, 2, 3, 0 }, { 1, 2, 3, 0 }, { 0, 1, 2, 3 }
	};
	
	protected static int[][] _nonElements = new int[][] {
            { 0, 1, 2, 3 }, { 1, 2, 3, 0 }, { 0, 2, 3, 0 }, { 2, 3, 0, 0 },
            { 0, 1, 3, 0 }, { 1, 3, 0, 0 }, { 0, 3, 0, 0 }, { 3, 0, 0, 0 },
            { 0, 1, 2, 0 }, { 1, 2, 0, 0 }, { 0, 2, 0, 0 }, { 2, 0, 0, 0 },
            { 0, 1, 0, 0 }, { 1, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }
			
	};

	protected static int[][] _predecessors = new int[][] {
            { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 2, 1, 0, 0 },
            { 0, 0, 0, 0 }, { 4, 1, 0, 0 }, { 4, 2, 0, 0 }, { 6, 5, 3, 0 },
            { 0, 0, 0, 0 }, { 8, 1, 0, 0 }, { 8, 2, 0, 0 }, { 10, 9, 3, 0 },
            { 8, 4, 0, 0 }, { 12, 9, 5, 0 }, { 12, 10, 6, 0 }, { 14, 13, 11, 7 },
	};

	protected static int[][] _successors = new int[][] {
            { 1, 2, 4, 8 }, { 3, 5, 9, 0 }, { 3, 6, 10, 0 }, { 7, 11, 0, 0 },
            { 5, 6, 12, 0 }, { 7, 13, 0, 0 }, { 7, 14, 0, 0 }, { 15, 0, 0, 0 },
            { 9, 10, 12, 0 }, { 11, 13, 0, 0 }, { 11, 14, 0, 0 }, { 15, 0, 0, 0 },
            { 13, 14, 0, 0 }, { 15, 0, 0, 0 }, { 15, 0, 0, 0 }, { 0, 0, 0, 0 }
	};
	
	protected float[][] _dotProducts = new float[4][4];
	protected float[]   _delta       = new float[16];
	protected float[][] _deltas      = new float[16][4];
	
	protected Entity _entity1;
	protected Entity _entity2;

	protected Vector3f[] _vertices1;
	protected Vector3f[] _vertices2;

	public GJK(Entity a, Entity b) {
		_entity1 = a;
		_entity2 = b;
		
		_vertices1 = new Vector3f[countVertices(_entity1)];
		for (int i = 0; i < _vertices1.length; ++i)
			_vertices1[i] = new Vector3f();

		_vertices2 = new Vector3f[countVertices(_entity2)];
		for (int i = 0; i < _vertices2.length; ++i)
			_vertices2[i] = new Vector3f();
	}
	
	protected int countVertices(Entity a) {
		int count = 0; 
		for (TriMesh t : a.getMeshes()) 
			count += t.getVertexCount();
		return count;
	}
	
	protected void setupVertices(Entity a, Vector3f[] v) {
		int i = 0;
		for (TriMesh t : a.getMeshes()) {
			FloatBuffer buf = t.getWorldCoords(null);
			
			for (; buf.hasRemaining(); ++i) {
				v[i].set(buf.get(), buf.get(), buf.get());
			}
		}
		
		//makeTest1();
		//makeTest2();
	}
	
	public float getDistance(Vector3f wpt1, Vector3f wpt2, SimplexPoint simplex, int seed) {
		// for now assume that the vertices have been set up properly
		// we will be needing to add a new method to Entity in order to 
		// gather this information in different ways for different objects.
		setupVertices(_entity1, _vertices1);
		setupVertices(_entity2, _vertices2);
		
		float sqrd = 0;
		float oldSqrd = 0;
		
		Vector3f displacementV = new Vector3f();
		Vector3f rDisplacementV = new Vector3f();
		
		boolean useDefault = true;
		boolean firstIteration = true;
		
		boolean computeBothWitnesses = wpt1 != null || wpt2 != null;
		
		if (simplex == null) {
			seed = 0;
			simplex = new SimplexPoint();
		}
		
		if (seed == 0) {
			simplex.simplex1[0] = 0;
			simplex.simplex2[0] = 0;
			simplex.numPoints = 1;
			simplex.lambdas[0] = 1;
			simplex.lastBest1 = 0;
			simplex.lastBest2 = 0;
			
			simplex.coords1[0] = _vertices1[0];
			simplex.coords2[0] = _vertices2[0];
		
		} else {
			/* If we are being told to use this seed point, there 
			 * is a good chance that the near point will be on
			 * the current simplex.  Besides, if we don't confirm
			 * that the seed point given satisfies the invariant
			 * (that the witness points given are the closest points
			 * on the current simplex) things can and will fall down.
			 */
			for (int v = 0; v < simplex.numPoints; ++v) {
				simplex.coords1[v] = _vertices1[simplex.simplex1[v]];
				simplex.coords2[v] = _vertices2[simplex.simplex2[v]];
			}
		}
		
		/* Now the main loop.  We first compute the distance between the 
		 * current simplicies, the check whether this gives the globally
		 * correct answer, and if not construct new simplices and try again.
		 */
		int maxIterations = _vertices1.length * _vertices2.length;
			// in practice we never see more than about 6 iterations. 
		for (int i = 0; i < maxIterations; ++i) {
			if (simplex.numPoints == 1) 
				simplex.lambdas[0] = 1;
			else {
				computeSubterms(simplex);
				
				if (useDefault) {
					useDefault = defaultDistance(simplex);
				}
				if (!useDefault) {
					backupDistance(simplex);
				}
			}
			
			//System.out.println("   computeBotWitnesses " + computeBothWitnesses);
			/* compute at least the displacement vectors given by the
			 * simplex structure.  If we are to provide both witness
			 * points, it's slightly faster to compute those first.
			 */
			if (computeBothWitnesses) {
				computePoint(simplex.coords1, simplex.lambdas);
				computePoint(simplex.coords2, simplex.lambdas);
				
				displacementV.set(wpt2.subtract(wpt1));
				rDisplacementV.set(displacementV.mult(-1));
			} else {
				displacementV.set(0,0,0);
				for (int p = 0; p < simplex.numPoints; ++p) {
					displacementV.x += simplex.lambdas[p] * (simplex.coords2[p].x - simplex.coords1[p].x);
					displacementV.y += simplex.lambdas[p] * (simplex.coords2[p].y - simplex.coords1[p].y);
					displacementV.z += simplex.lambdas[p] * (simplex.coords2[p].z - simplex.coords1[p].z);
				}
				rDisplacementV.set(displacementV.mult(-1));
			}
			
			sqrd = displacementV.dot(displacementV);
			//System.out.println("  disp: " + displacementV + " rdisp" + rDisplacementV);
			//System.out.println("  sqrd: " + sqrd);
			
			/* if we are using a c-space simplex with DIM_PLUS_ONE
			 * points, this is interior to the simplex, and indicates
			 * that the original hulls overlap, as does the distance 
			 * between them being to small.
			 */
			if (sqrd < 1e-8) {
				//System.out.println("sqrd < 1e-8");
				return 1e-8f;
			}
			
			/* find the point in obj1, that is maximal in the
			 * direction displacement, and the point in obj2 that 
			 * is minimal in direction displacement.
			 */
			SimpleResult sr1 = supportSimple(_vertices1, displacementV);
			SimpleResult sr2 = supportSimple(_vertices2, rDisplacementV);
			
			float gVal = sqrd + sr1.value + sr2.value;
			
			if (gVal < 0) {
				//System.out.println("Should not happen gVal < 0");
				gVal = 0;
			}
			
			if (gVal < 1e-8) {
				//System.out.println("gVal < 1e-8");
				return sqrd;
			}
			
			// check for good calculation above
			if ((firstIteration || (sqrd < oldSqrd)) && simplex.numPoints <= DIM) {
				/* Normal case: add the new c-space points to the current
				 * simplex, and call simplexDistance()
				 */
				simplex.simplex1[simplex.numPoints] = simplex.lastBest1 = sr1.index;
				simplex.simplex2[simplex.numPoints] = simplex.lastBest2 = sr2.index;
				simplex.lambdas[simplex.numPoints] = 0;
				
				simplex.coords1[simplex.numPoints].set(_vertices1[sr1.index]);
				simplex.coords2[simplex.numPoints].set(_vertices2[sr2.index]);
				
				++simplex.numPoints;
				
				oldSqrd = sqrd;
				firstIteration = false;
				useDefault = true;
				continue;
			}
			
			if (useDefault) 
				useDefault = false;
			else {
				// give up trying
				//System.out.println("giving up");
				return sqrd;
			}
		} // end of while
		
		return 0;
	}

	/**
	 * default distance is our equivalent of GJK's distance subalgorithm
	 * It is given a c-space simplex as indices of size (up to DIM_PLUS_ONE) points
	 * in the master point list, and computes a pair of witness points for the 
	 * minimum distance vector between the simplices.  This vector is indicated
	 * by setting the values lambdas[] in the given array, and returning the number of 
	 * non-zero values of lambdas
	 * @param simplex
	 * @return
	 */
	protected boolean defaultDistance(SimplexPoint simplex) {
		int s = 1;
		boolean ok = false;
		
		//System.out.println("    (defaultDistance)");
		for (s = 1; s < TWICE_TWO_TO_DIM && _maxElts[s] < simplex.numPoints; ++s) {

			_delta[s] = 0;
			ok = true;
			
			for (int j = 0; ok && j < _cardinality[s]; ++j) {
				int elts = _elements[s][j];
				if (_deltas[s][elts] > 0) {
					//System.out.println("        " + s + " inc-amount " + _deltas[s][elts]);
					_delta[s] += _deltas[s][elts];
				} else {
					ok = false;
				}
			}
			
			for (int k = 0; ok && k < (simplex.numPoints - _cardinality[s]); ++k) {
				int succ = _successors[s][k];
				int nonElts = _nonElements[s][k];
					
				//System.out.println("       " + s + " " + k + " succ " + succ + " non-elts " + nonElts);
				if (_deltas[succ][nonElts] > 0)
					ok = false;
			}
			
			if (ok && _delta[s] >= 1e-20) {
				break;
			}
		}
		
		//System.out.println("       s " + s);
		if (ok) {
			resetSimplex(s, simplex);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * A version of GJK's `Backup Procedure'. 
	 * Note that it requires that the _delta[s] entries have been 
	 * computed for all viable s within simplex_distance
	 * @param simplex
	 */
	protected void backupDistance(SimplexPoint simplex) {
		float[] distSqNum = new float[TWICE_TWO_TO_DIM];
		float[] distSqDen = new float[TWICE_TWO_TO_DIM];
		
		int bests = 0;
		for (int s = 1; s < TWICE_TWO_TO_DIM && _maxElts[s] < simplex.numPoints; ++s) {
			if (_delta[s] > 0) {
				
				int i = 0;
				for (i = 0; i < _cardinality[s]; ++i) {
					if ( _deltas[s][_elements[s][i]] <= 0) 
						break;
				}
				
				if (i < _cardinality[s])
					continue;
				
				distSqNum[s] = 0;
				for (int j = 0; j < _cardinality[s]; ++j) {
					for (int k = 0; k < _cardinality[s]; ++k) {
						distSqNum[s] += _deltas[s][_elements[s][j]] * _deltas[s][_elements[s][k]] *
								_dotProducts[_elements[s][j]][_elements[s][k]];
					}
				}
				
				distSqDen[s] = _delta[s] * _delta[s];
				
				float val1 = distSqNum[s] * distSqDen[bests];
				float val2 = distSqNum[bests] * distSqDen[s];
				if (bests < 1 || val1 < val2)
					bests = s;
			}
		}
		resetSimplex(bests, simplex);
	}
	
	/**
	 * compute the lambda values that indicate exactly where the
	 * witness points lie.  We also fold back the values stored for the
	 * indices into the original point arrays, and the transformed
	 * coordinates, so that tehse are ready for subsequent calls.
	 * @param subset
	 * @param simplex
	 */
	protected void resetSimplex(int subset, SimplexPoint simplex) {
		//System.out.println("       (reset-simplex) " + subset);
		for (int j = 0; j < _cardinality[subset]; ++j) {
			int oldPos = _elements[subset][j];
			//System.out.println("        oldPos " + oldPos + " j " + j);
			if (oldPos != j) {
				simplex.simplex1[j] = simplex.simplex1[oldPos];
				simplex.simplex2[j] = simplex.simplex2[oldPos];
				
				// we need to copy the actual coordinate values as well
				simplex.coords1[j].set(simplex.coords1[oldPos]);
				simplex.coords2[j].set(simplex.coords2[oldPos]);
			}
			
			//System.out.println("        delta " + _deltas[subset][_elements[subset][j]] + " div-delta " + 
			//               _delta[subset]);
			simplex.lambdas[j] = _deltas[subset][_elements[subset][j]] / _delta[subset];
		}
		simplex.numPoints = _cardinality[subset];
	}
	
	/**
	 * The simplex_distance routine requires the computation of a number of
	 * delta terms.  These are computed here.
	 * @param simplex
	 */
	protected void computeSubterms(SimplexPoint simplex) {
		Vector3f[] cSpacePoints = new Vector3f[DIM_PLUS_ONE];
		for (int i = 0; i < cSpacePoints.length; ++i)
			cSpacePoints[i] = new Vector3f();
		
		for (int i = 0; i < simplex.numPoints; ++i) {
			cSpacePoints[i].set(simplex.coords1[i].subtract(simplex.coords2[i]));
			//System.out.println("     cSpacePoints " + cSpacePoints[i]);
		}
		
		for (int i = 0; i < simplex.numPoints; ++i) {
			for (int j = i; j < simplex.numPoints; ++j) {
				_dotProducts[i][j] = _dotProducts[j][i] = cSpacePoints[i].dot(cSpacePoints[j]);
				//System.out.println(".....dotProducts " + _dotProducts[i][j]);
			}
		}
		
		for (int s = 1; s < TWICE_TWO_TO_DIM && _maxElts[s] < simplex.numPoints; ++s) {
			
			// just record _deltas[s][_elements[s][0]]
			if (_cardinality[s] <= 1) {
				_deltas[s][_elements[s][0]] = 1;
				//System.out.println("     " + s + " card 1 elts " + _elements[s][0]);
				continue;
			}
			
			// the base case for the recursion
			if (_cardinality[s] == 2) {
				_deltas[s][_elements[s][0]] = 
					_dotProducts[_elements[s][1]][_elements[s][1]] - 
					_dotProducts[_elements[s][1]][_elements[s][0]];
				
				_deltas[s][_elements[s][1]] = 
					_dotProducts[_elements[s][0]][_elements[s][0]] - 
					_dotProducts[_elements[s][0]][_elements[s][1]];

				//System.out.println("     " + s + " card = 2 elts[0] " + _elements[s][0] + " delta " + _deltas[s][_elements[s][0]] + 
				//		" elts[1] " + _elements[s][1] + " delta " + _deltas[s][_elements[s][1]]);
				continue;
			}
			
			// otherwise _cardinality[s] > 2, so use the general case
			// for each element of this subset s, namely elements(s,j)
			for (int j = 0; j < _cardinality[s]; ++j) {
				int jelt = _elements[s][j];
				int jsubset = _predecessors[s][j];
				
				float sum = 0;
				// for each element of subset jsubset
				for (int i = 0; i < _cardinality[jsubset]; ++i) {
					int ielt = _elements[jsubset][i];
					sum += _deltas[jsubset][ielt] * 
						(_dotProducts[ielt][_elements[jsubset][0]] - _dotProducts[ielt][jelt]);
				}
				_deltas[s][jelt] = sum;
			}
		}
	}
	
	/**
	 * computes the coordinates of a simplex point.
	 * @param vertices
	 * @param lambdas
	 * @return
	 */
	protected Vector3f computePoint(Vector3f[] vertices, float[] lambdas) {
		Vector3f finalVector = new Vector3f();
		
		for (int i = 0; i < vertices.length; ++i) {
			finalVector.x += (vertices[i].x * lambdas[i]);
			finalVector.y += (vertices[i].y * lambdas[i]);
			finalVector.z += (vertices[i].z * lambdas[i]);
		}
		return finalVector;
	}
	
	protected SimpleResult supportSimple(Vector3f[] vertices, Vector3f direction) {
		//System.out.println("    supportSimple " + direction);
		SimpleResult sr = new SimpleResult();
		
		for (int i = 0; i < vertices.length; ++i) {
			float val = vertices[i].dot(direction);
			if (sr.index == -1 || val > sr.value) {
				sr.index = i;
				sr.value = val;
			}
		}
		
		return sr;
	}
	
	protected void makeTest1() {
		_vertices1 = new Vector3f[8];
		_vertices1[0] = new Vector3f(0,2,0);
		_vertices1[1] = new Vector3f(1,2,0);
		_vertices1[2] = new Vector3f(1,2,1);
		_vertices1[3] = new Vector3f(0,2,1);
		_vertices1[4] = new Vector3f(0,3,0);
		_vertices1[5] = new Vector3f(1,3,0);
		_vertices1[6] = new Vector3f(1,3,1);
		_vertices1[7] = new Vector3f(0,3,1);
	}
	
	protected void makeTest2() {
		_vertices2 = new Vector3f[8];
		_vertices2[0] = new Vector3f(2,0,0);
		_vertices2[1] = new Vector3f(3,0,0);
		_vertices2[2] = new Vector3f(3,0,1);
		_vertices2[3] = new Vector3f(2,0,1);
		_vertices2[4] = new Vector3f(2,1,0);
		_vertices2[5] = new Vector3f(3,1,0);
		_vertices2[6] = new Vector3f(3,1,1);
		_vertices2[7] = new Vector3f(2,1,1);
	}

	public static void main(String[] args) {
		GJK gjk = new GJK(null, null);
		//System.out.println("Distance: " + gjk.getDistance(null, null, null, 0));
	}
}

class SimpleResult {
	public float value;
	public int index = -1;
}

class SimplexPoint {

	public int numPoints;
	
	public int[] simplex1 = new int[4];
	public int[] simplex2 = new int[4];
	
	public float[] lambdas = new float[4];
	
	public Vector3f[] coords1 = new Vector3f[4];
	public Vector3f[] coords2 = new Vector3f[4];
	
	public int lastBest1;
	public int lastBest2;
	
	public float errorVal;
	
	public SimplexPoint() {
		for (int i = 0; i < 4; ++i) {
			coords1[i] = new Vector3f();
			coords2[i] = new Vector3f();
		}
	}
}