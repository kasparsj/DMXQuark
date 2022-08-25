DmxPlayer { // PatternPlayer
	var <id;
	var <moveSpeed;
	var <fadeSpeed;
	var <players;
	classvar <defaultInst;
	classvar <all;

	*initClass {
		all = ();
	}

	*default {
		if (defaultInst.isNil, {
			defaultInst = DmxPlayer.new;
		});
		^defaultInst;
	}

	*new { |myid = \default|
		^super.new.init(myid);
	}

	init { |myid|
		id = myid;
		moveSpeed = Bus.control(Server.default);
		moveSpeed.set(1);
		fadeSpeed = Bus.control(Server.default);
		fadeSpeed.set(1);
		players = List();

		all.add(myid -> this);
		if (defaultInst == nil, {
			defaultInst = this;
		});

		CmdPeriod.doOnce({
			defaultInst = nil;
		});
	}

	makeDefault {
		defaultInst = this;
	}

	play { |pattern|
		var evPlayer;
		if (pattern.isKindOf(Symbol), {
			pattern = Pdef.all[pattern];
		});
		if (pattern.isKindOf(Pattern).not, {
			"pattern not found in %".format(id).throw;
		});
		evPlayer = pattern.play(nil, (player: this));
		players.add(evPlayer);
		^evPlayer;
	}

	stop { |evPlayers|
		if (evPlayers.isKindOf(SequenceableCollection).not, {
			evPlayers = [evPlayers];
		});
		evPlayers.do { |evPlayer|
			if (evPlayer.isKindOf(Symbol), {
				if (Pdef.all[evPlayer].notNil and: { Pdef(evPlayer).player.notNil }, {
					players.remove(Pdef(evPlayer).player);
					Pdef(evPlayer).stop;
				});
			});
			if (evPlayer.isKindOf(EventStreamPlayer) and: { players.indexOf(evPlayer).notNil }, {
				evPlayer.stop;
				players.remove(evPlayer);
			});
		};
	}

	stopAll {
		players.do { |player|
			player.stop;
		};
		players.clear;
	}
}