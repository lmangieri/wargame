package basic;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import objetives.ConquerContinentsObjetive;
import objetives.ConquerTwentyFourCountriesObjetive;
import objetives.PutTwoPiecesOnEighteenCountriesObjetive;
import algorithm.GraphAlgorithm;
import algorithm.StrategiesAlgorithm;
import auxiliaryEntities.AuxPutOrRelocatePiece;
import auxiliaryEntities.StructureAuxToPutPiecesOnContinent;
import entities.Node;
import entities.Player;

public class StateMachine implements Runnable {
	public GameExecutor gameExecutor;

	public GraphAlgorithm graphAlgorithm;
	public StrategiesAlgorithm strategiesAlgorithm;
	private Thread state2Thread;

	public int velocityChoosed;

	public Player currentPlayer;
	public int currentIndex;

	private int stateGame;

	// Variables for state 3.
	public Node nodeTarget;
	public Node nodeAttacker;
	public boolean isFirstRound;
	public int numberOfPiecesToPut;

	// Variables for state 6
	public List<StructureAuxToPutPiecesOnContinent> listOfStructureAuxToPutPiecesOnContinent;

	// Variables for state 7
	public List<AuxPutOrRelocatePiece> listOfAuxPutOrRelocatePiece;
	public Node nodeToTransferOrigin;
	public Node nodeToTransferDestiny;

	public StateMachine(GameExecutor gameExecutor) {
		this.gameExecutor = gameExecutor;
		graphAlgorithm = new GraphAlgorithm(this.gameExecutor.getGraph());
		strategiesAlgorithm = new StrategiesAlgorithm(
				this.gameExecutor.getGraph());
		isFirstRound = true;
	}

	public void initGameStateOne() {
		System.out.println("initGameStateOne");
		this.currentIndex = 0;

		this.setStateGame(2);
		stage2();
	}

	// TODO : urgente... separar em dois estados...
	public void stage2() {
		if (this.gameExecutor.getTotalPlayers() == this.currentIndex) {
			this.isFirstRound = false;
		}
		Player p = this.gameExecutor.getPlayers().get(
				this.currentIndex % this.gameExecutor.getTotalPlayers());
		this.currentPlayer = p;

		if (theGameHasEnded()) {
			this.setStateGame(8);
			this.stage8();
		} else if (!this.isFirstRound) { // fase to put pieces
			int np = this.currentPlayer.getNodes().size() / 2;
			if (np < 3) {
				np = 3;
			}
			this.numberOfPiecesToPut = np;

			this.setStateGame(5);

			if (!p.isPlayer()) {
				this.setStateGame(6);
				this.gameExecutor
						.putPieceToComputer(p, this.numberOfPiecesToPut);
				this.numberOfPiecesToPut = 0;
				this.stage6();
			}

		} else {
			this.setStateGame(3);
			this.stage3();
		}
	}

	// estagio colocar peças de continente. used only by computer
	public void stage6() {
		Player p = this.gameExecutor.getPlayers().get(
				this.currentIndex % this.gameExecutor.getTotalPlayers());
		this.currentPlayer = p;
		this.gameExecutor.putPiecesOnContinentForComputer(p);

		this.setStateGame(3);
		this.stage3();
	}

	// estagio de ataque....
	public void stage3() {
		Player p = this.gameExecutor.getPlayers().get(
				this.currentIndex % this.gameExecutor.getTotalPlayers());

		if (p.isPlayer()) {

			currentPlayer = p;
			this.setStateGame(3);
		} else {
			this.setStateGame(4);
			stage4(p);
		}
	}

	public void stage4(Player p) {
		Iterator<Node> iterator = p.getNodes().iterator();

		while (iterator.hasNext()) {
			Node n = iterator.next();
			while (n.canAttack()) {
				Node target = strategiesAlgorithm.decideNextTarget(n);
				if (target != null) {
					changeStateOfNodeAttackerAndNodeTarget(n, target);
					try {
						Thread.sleep(velocityChoosed);
					    this.gameExecutor.d = null;
					} catch (InterruptedException e) {
					}
					this.gameExecutor
							.attack(this.nodeAttacker, this.nodeTarget);

					changeStateOfNodeAttackerAndNodeTarget(null, null);
				} else {
					break;
				}

			}
		}
		
		try {
			Thread.sleep(velocityChoosed);
		    this.gameExecutor.d = null;
		} catch (InterruptedException e) {
		}
		
		if (!theGameHasEnded()) {
			this.setStateGame(7);
			this.stage7();
		} else {
			this.setStateGame(8);
			this.stage8();
		}
	}

	private synchronized void changeStateOfNodeAttackerAndNodeTarget(Node na,
			Node nt) {
		this.nodeAttacker = na;
		this.nodeTarget = nt;
	}


	// estágio de transferência do computador....
	public synchronized void stage7() {
		listOfAuxPutOrRelocatePiece = AuxPutOrRelocatePiece
				.getStructureToRelocatePieces(this.currentPlayer);

		ConcurrentLinkedQueue<Node> targets = this.currentPlayer.getObjetive().getTargets();

		List<String> wayToTarget = null;
		for (AuxPutOrRelocatePiece aux : listOfAuxPutOrRelocatePiece) {
			if (aux.getPiecesThatCanBeRelocated() > 0
					&& aux.getNode().isLocked()) {
				for (Node target : targets) {
					if (!aux.getNode().getPlayer().equals(target.getPlayer())) {
						wayToTarget = this.graphAlgorithm.wayToTarget(aux
								.getNode().getName(), target.getName());

						Node nodeToTransfer = gameExecutor.getGraph()
								.getNodeByName(wayToTarget.get(1));
						nodeToTransfer.setNumberOfPieces(nodeToTransfer
								.getNumberOfPieces()
								+ aux.getPiecesThatCanBeRelocated());

						Node nodeOrigin = aux.getNode();
						nodeOrigin.setNumberOfPieces(nodeOrigin
								.getNumberOfPieces()
								- aux.getPiecesThatCanBeRelocated());

						aux.setPiecesThatCanBeRelocated(0);
					}
				}
			}
		}

		nextTurn();

	}

	public void nextTurn() {
		this.currentIndex = this.currentIndex + 1;
		state2Thread = new Thread(this);
		state2Thread.start();
	}

	// FIM DE JOGO
	public void stage8() {
		if (currentPlayer.isPlayer()) {
			GameInterface.victoryMusic.doIt();
		} else {
			GameInterface.failMusic.doIt();
		}
		System.out.println("Vencedor = "
				+ currentPlayer.getColorEnum().getName());
	}


	public boolean theGameHasEnded() {
		if (this.currentPlayer.getObjetive().getClass()
				.equals(ConquerTwentyFourCountriesObjetive.class)) {
			if (this.currentPlayer.getNodes().size() > 23) {
				System.out.println("conquer 24 countries....");
				System.out.println("the game has ended");
				this.setStateGame(8);
				return true;
			}
		} else if (this.currentPlayer.getObjetive().getClass()
				.equals(PutTwoPiecesOnEighteenCountriesObjetive.class)) {
			if (this.currentPlayer.getNodes().size() > 17) {
				int count = 0;
				for (Node n : this.currentPlayer.getNodes()) {
					if (n.getNumberOfPieces() > 1) {
						count = count + 1;
					}
				}
				if (count > 17) {
					System.out.println("More than 17 have two pieces....");
					System.out.println("the game has ended");
					return true;
				} else {
					return false;
				}
			}
		} else if (this.currentPlayer.getObjetive().getClass()
				.equals(ConquerContinentsObjetive.class)) { // preciso
															// verificar... pois
															// talvez todos os
															// targets já tenham
															// sido pegos...
			for (Node target : this.currentPlayer.getObjetive().getTargets()) {
				if (!target.getPlayer().equals(this.currentPlayer)) {
					return false;
				}
			}
			System.out.println("the game has ended");
			this.setStateGame(8);
			return true;

		} else if (this.currentPlayer.getObjetive().getTargets().isEmpty()) {
			System.out.println("the game has ended");
			this.setStateGame(8);
			return true;
		}
		return false;
	}

	public synchronized int getStateGame() {
		return stateGame;
	}

	public synchronized void setStateGame(int stateGame) {
		this.stateGame = stateGame;
	}

	@Override
	public void run() {
		this.setStateGame(2);
		this.stage2();
	}
}
