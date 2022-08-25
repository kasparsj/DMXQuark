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

	value { |... args|
		^func.value(*args);
	}

	valueWithEnvir { |env|
		^func.valueWithEnvir(env);
	}
}