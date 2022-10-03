package net.buddat.ludum.ld51;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.buddat.ludum.ld51.LD51Game;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("LD51");
		config.setWindowedMode(948, 533);
		config.setResizable(false);
		new Lwjgl3Application(new LD51Game(), config);
	}
}
