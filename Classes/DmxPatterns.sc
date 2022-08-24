PdmxScene : Pattern {
	var <>patternpairs;

	*new { arg ... pairs;
		if (pairs.size.odd, { Error("Pmono should have even number of args.\n").throw; });
		^super.newCopyArgs(pairs);
	}

	storeArgs { ^patternpairs }

	embedInStream { arg inval;
		var dic = Dictionary.newFrom(patternpairs);
		var patcherId = dic[\patcher] ? \default;
		var patcher = if (patcherId.isSymbol, {
			if (patcherId == \default, {
				DmxPatcher.default;
			}, {
				DmxPatcher.all[patcherId]
			});
		}, { patcherId });
		var groupName = dic[\group];
		var group = if (groupName.isSymbol, { patcher.groups[groupName] }, { groupName });
		var fixtures = dic[\fixtures] ? dic[\fixture];
		if (patcher.isNil, {
			"patcher % not found".format(patcherId).throw;
		});
		[\patcher, \group, \fixtures, \fixture].do { |key|
			dic.removeAt(key);
		};
		if (group.notNil, {
			if (fixtures.notNil, {
				if (fixtures.isSequenceableCollection.not, { fixtures = [fixtures]; });
				fixtures.do { |fixture|
					fixture = if (fixture.isInteger, { group.fixtures[fixture] }, { fixture });
					dic.keysValuesDo { |key, value|
						fixture.set(key, value);
					};
				};
			}, {
				dic.keysValuesDo { |key, value|
					group.set(key, value);
				};
			});
		}, {
			"group % not found in patcher %".format(group, patcher.id).postln;
		});
		^inval;
	}
}

PdmxChase : Pattern {
	var <chaseDef, <>patternpairs;
	var <players;

	*new { arg name ... pairs;
		if (pairs.size.odd, { Error("PdmxChase should have odd number of args.\n").throw; });
		^super.newCopyArgs(DmxChaseDef(name.asSymbol), pairs, List())
	}

	embedInStream { | inevent |
		var chasePatt = chaseDef.value(*patternpairs);
		^chasePatt.embedInStream(inevent);
	}

	play { arg clock, protoEvent, quant;
		var env = chaseDef.envir(*patternpairs);
		var evPlayer = this.asEventStreamPlayer(protoEvent).play(clock, false, quant);
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
