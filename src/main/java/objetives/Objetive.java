package objetives;

import java.awt.Image;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import entities.Node;

public interface Objetive {
	public ConcurrentLinkedQueue<Node> getTargets();
	public void setIdentifier(int identifier);
	public int getIdentifier();
	
	public void setImageObjetive(Image image);
	public Image getImageObjetive();
	
	
	// existem 4 tipos de objetivos... 
	// 1 deles possui uma lista de continentes
	// 1 deles possui uma cor  para ser atacada
	// 1 deles para conquistar 18 territórios e colocar 2 peças em cada
	// 1 deles para conquistar 24 territórios...
}