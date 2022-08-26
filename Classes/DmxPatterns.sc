PdmxScene : Pbind {
	*new { arg ... pairs;
		if (pairs.size.odd, { Error("PdmxScene should have even number of args.\n").throw; });
		^super.newCopyArgs(pairs);
	}

	embedInStream { |inevent|
		var dict = Dictionary.newFrom(patternpairs);
		dict.put(\type, \dmx);
		if (dict[\dur].isNil and: { dict[\sustain].isNil and: { dict[\delta].isNil } }, {
			dict = Dictionary.newFrom([\delta, Pseq([0], 1)] ++ dict.asKeyValuePairs);
		});
		patternpairs = dict.asKeyValuePairs;
		^super.embedInStream(inevent);
	}
}

PdmxChase : Pattern {
	var <chaseDef, <>patternpairs;
	var <players;

	*new { arg name ... pairs;
		var chaseDef = DmxChaseDef(name.asSymbol);
		if (pairs.size.odd, { Error("PdmxChase should have odd number of args.\n").throw; });
		if (chaseDef.isNil, { Error("DmxChaseDef % not found.\n").format(name).throw; });
		^super.newCopyArgs(chaseDef, pairs, List());
	}

	storeArgs { ^patternpairs }

	embedInStream { | inevent |
		var pattern;
		var env = this.prInitEnv(inevent);
		this.prSetPlayer(env);
		this.prSetFixtures(env);
		pattern = chaseDef.valueWithEnvir(env);
		^pattern.embedInStream(inevent);
	}


	prInitEnv { |inevent|
		var env;
		var streampairs = patternpairs.copy;
		var endval = streampairs.size - 1;

		forBy (1, endval, 2) { arg i;
			streampairs.put(i, streampairs[i].asStream);
		};

		env = Environment.newFrom(inevent.asKeyValuePairs);
		forBy (0, endval, 2) { arg i;
			var name = streampairs[i];
			var stream = streampairs[i+1];
			var streamout = stream.next(env);
			if (streamout.isNil) { ^inevent };

			if (name.isSequenceableCollection) {
				if (name.size > streamout.size) {
					("the pattern is not providing enough values to assign to the key set:" + name).warn;
					^inevent
				};
				name.do { arg key, i;
					env.put(key, streamout[i]);
				};
			}{
				env.put(name, streamout);
			};
		};
		^env;
	}

	prSetPlayer { |env|
		var playerId = env[\player] ? \default;
		env[\player] = if (playerId.isSymbol, { DmxPlayer.all[playerId]; }, { playerId });
		if (env[\player].isNil, {
			"player % not found".format(playerId).postln;
		});
	}

	prSetFixtures { |env|
		if (env[\fixtures].isNil, {
			var group = env[\group];
			var patcherId = env[\patcher] ? \default;
			var patcher = if (patcherId.isSymbol, {
				if (patcherId == \default, {
					DmxPatcher.default;
				}, {
					DmxPatcher.all[patcherId]
				});
			}, { patcherId });
			if (patcher.isNil, {
				"patcher % not found".format(patcherId).throw;
			});
			if (group.isKindOf(Symbol), {
				var groupName = group;
				group = patcher.groups[groupName];
				if (group.isNil, {
					"group % not found".format(groupName).throw;
				});
			});
			env.put(\fixtures, group.fixtures);
		});
	}

	play { arg clock, protoEvent, quant;
		var dmxPlayer = if (protoEvent.notNil, { protoEvent[\player] }), evPlayer;
		if (dmxPlayer.isNil) {
			var dict = Dictionary.newFrom(patternpairs);
			this.prSetPlayer(dict);
			dmxPlayer = dict[\player];
		};
		evPlayer = this.asEventStreamPlayer(protoEvent).play(clock, false, quant);
		players.add(evPlayer);
		dmxPlayer.players.add(evPlayer)
		^evPlayer;
	}

	stop {
		var dict = Dictionary.newFrom(patternpairs);
		this.prSetPlayer(dict);
		dict[\player].stop(players);
		players.clear;
	}

	isPlaying {
		^(players.size > 0);
	}
}
