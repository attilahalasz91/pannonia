package hu.halasz.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import hu.halasz.HmPoc;
import hu.halasz.Pannonia;
import hu.halasz.PannoniaVoroi;
import hu.halasz.ShaderTest;
import hu.halasz.ShaderTest2;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1085;
		config.height = 700;
		new LwjglApplication(new HmPoc(), config);
	}
}
