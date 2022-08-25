PdmxScene : Pbind {
	*new { arg ... pairs;
		if (pairs.size.odd, { Error("PdmxScene should have even number of args.\n").throw; });
		^super.newCopyArgs(pairs);
	}

	embedInStream { |inevent|
		var dict = Dictionary.newFrom(patternpairs);
		dict.put(\type, \dmx);
		if (dict[\dur].isNil and: { dict[\sustain].isNil and: { dict[\delta].isNil } }, {
			dict.put(\delta, Pseq([0], 1));
		});
		patternpairs = dict.asKeyValuePairs;
		^super.embedInStream(inevent);
	}
}

PdmxChase : Pattern {
	var <chaseDef, <>patternpairs;
	var <players;

	*new { arg name ... pairs;
		if (pairs.size.odd, { Error("PdmxChase should have odd number of args.\n").throw; });
		^super.newCopyArgs(DmxChaseDef(name.asSymbol), pairs, List())
	}

	storeArgs { ^patternpairs }

	embedInStream { | inevent |
		var chasePatt = chaseDef.value(*patternpairs);
		^chasePatt.embedInStream(inevent);
	}

	play { arg clock, protoEvent, quant;
		var env, evPlayer;
		var streampairs = patternpairs.copy;

		forBy (1, (streampairs.size - 1), 2) { arg i;
			streampairs.put(i, streampairs[i].asStream);
		};

		env = chaseDef.envir(*streampairs);
		evPlayer = this.asEventStreamPlayer(protoEvent).play(clock, false, quant);
		players.add(evPlayer);
		env[\player].players.add(evPlayer)
		^evPlayer;
	}

	stop {
		var env = chaseDef.envir(*patternpairs);
		env[\player].stop(players);
		players.clear;
	}

	isPlaying {
		^(players.size > 0);
	}
}
