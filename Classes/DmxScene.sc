DmxScene {
	var <name;
	var <func;
	classvar <all;

	*initClass {
		all = ();
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

	play { |...args|
		var env = Environment.newFrom(args), evPlayer;
		if (env[\fixtures].isNil, {
			var group = env[\group];
			if (group.isKindOf(Symbol), {
				group = DmxPatcher.default.groups[group];
			});
			if (group.isNil, {
				"group % not found".format(group).postln;
			}, {
				env.put(\fixtures, group.fixtures);
			});
		});
		func.valueWithEnvir(env);
	}

}