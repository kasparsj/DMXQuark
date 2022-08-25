PdmxScene : Pattern {
	var <>patternpairs;

	*new { arg ... pairs;
		if (pairs.size.odd, { Error("Pmono should have even number of args.\n").throw; });
		^super.newCopyArgs(pairs);
	}

	storeArgs { ^patternpairs }

	embedInStream { arg inevent;
		var dict, patcherId, patcher;
		var groupName, group, fixtures;
		var streampairs = patternpairs.copy;
		var endval = (streampairs.size - 1);
		var event;

		forBy (1, endval, 2) { arg i;
			streampairs.put(i, streampairs[i].asStream);
		};

		inevent.put(\type, \dmx);
		if (inevent.isNil) { ^nil.yield };
		event = inevent.copy;
		forBy (0, endval, 2) { arg i;
			var name = streampairs[i];
			var stream = streampairs[i+1];
			var streamout = stream.next(event);
			if (streamout.isNil) { ^inevent };

			if (name.isSequenceableCollection) {
				if (name.size > streamout.size) {
					("the pattern is not providing enough values to assign to the key set:" + name).warn;
					^inevent
				};
				name.do { arg key, i;
					event.put(key, streamout[i]);
				};
			}{
				event.put(name, streamout);
			};
		};
		inevent = event.yield;
		^event;
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
