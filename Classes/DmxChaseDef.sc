DmxChaseDef {
	var <name;
	var <func;
	classvar <all;

	*initClass {
		all = ();

		StartUp.add {
			// load dmx synthDefs
			(Quark("DMXQuark").localPath ++ "/SynthDefs/*.scd").loadPaths;
			// load core chaseDefs
			(Quark("DMXQuark").localPath ++ "/ChaseDefs/*.scd").loadPaths;
		};
	}

	*new { |myName, myFunc|
		if (myFunc.isNil, {
			^all[myName.asSymbol];
		}, {
			^super.new.init(myName, myFunc);
		});
	}

	init { |myName, myFunc|
		name = myName.asSymbol;
		func = myFunc;
		all[name] = this;
	}

	value { |...args|
		var isArgsArray = args.clump(2).flop[0].every({ |item| item.class == Symbol }) && args.size.even;
		^if (isArgsArray, {
			var env = this.envir(*args);
			func.valueWithEnvir(env);
		}, {
			func.value(*args);
		});
	}

	envir { |...args|
		var env = Environment.newFrom(args);
		var playerId = env[\player] ? \default;
		env[\player] = if (playerId.isSymbol, { DmxPlayer.all[playerId]; }, { playerId });
		if (env[\player].isNil, {
			"player % not found".format(playerId).throw;
		});
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
		^env;
	}
}