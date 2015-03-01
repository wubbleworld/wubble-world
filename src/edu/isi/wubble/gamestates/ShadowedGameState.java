package edu.isi.wubble.gamestates;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.pass.BasicPassManager;
import com.jme.renderer.pass.RenderPass;
import com.jme.renderer.pass.ShadowedRenderPass;
import com.jme.system.DisplaySystem;

import edu.isi.wubble.Main;

public abstract class ShadowedGameState extends VisualGameState {

	protected BasicPassManager _passManager;
	protected ShadowedRenderPass _shadowPass;
	
	public ShadowedGameState() {
		super();
		
		_shadowPass = new ShadowedRenderPass();
		_shadowPass.setRenderShadows(true);
		//_shadowPass.setRenderVolume(true);
        _shadowPass.setLightingMethod(ShadowedRenderPass.MODULATIVE);
		
		RenderPass rPass = new RenderPass();
		rPass.add(_fpsNode);

		_passManager = new BasicPassManager();
		_passManager.add(_shadowPass);
		_passManager.add(rPass);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		_passManager.updatePasses(tpf);
	}
	
	public void render(float tpf) {
        _passManager.renderPasses(DisplaySystem.getDisplaySystem().getRenderer());
	}
}	
