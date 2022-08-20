DmxChase {
	var <name;
	var <func;
	var <players;
	classvar <all;

	*initClass {
		Class.initClassTree(SynthDef);

		SynthDef(\dmx_sine, { |out = 0, freq = 1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, SinOsc.kr(freq * speed, phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_sine2, { |out = 0, freq_0, freq_1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, SinOsc.kr([freq_0 * speed, freq_1 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_sine3, { |out = 0, freq_0, freq_1, freq_2, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, SinOsc.kr([freq_0 * speed, freq_1 * speed, freq_2 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_circle, { |out = 0, freq = 1, speed = 1, phase = 0, cx = 0.5, cy = 0.5, radius = 0.5|
			var x = SinOsc.kr(freq * speed, phase).range(-1 * radius, radius);
			var y = SinOsc.kr(freq * speed, phase - pi/2).range(-1 * radius, radius);
			Out.kr(out, [cx + x, cy + y].fold(0, 1));
		}).add;

		SynthDef(\dmx_saw, { |out=0, freq = 1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFSaw.kr(freq * speed, phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_saw2, { |out=0, freq_0, freq_1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFSaw.kr([freq_0 * speed, freq_1 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_saw3, { |out=0, freq_0, freq_1, freq_2, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFSaw.kr([freq_0 * speed, freq_1 * speed, freq_2 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_tri, { |out=0, freq = 1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFTri.kr(freq * speed, phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_tri2, { |out=0, freq_0, freq_1, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFTri.kr([freq_0 * speed, freq_1 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_tri3, { |out=0, freq_0, freq_1, freq_2, speed = 1, phase = 0, lo = 0, hi = 1|
			Out.kr(out, LFTri.kr([freq_0 * speed, freq_1 * speed, freq_2 * speed], phase).range(lo, hi));
		}).add;

		SynthDef(\dmx_noise, { |out=0, freq = 1, speed = 1, lo = 0, hi = 1|
			Out.kr(out, LFNoise2.kr(freq * speed).range(lo, hi));
		}).add;

		SynthDef(\dmx_noise2, { |out=0, freq_0, freq_1, speed = 1, lo = 0, hi = 1|
			Out.kr(out, LFNoise2.kr([freq_0 * speed, freq_1 * speed]).range(lo, hi));
		}).add;

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
		players = List();
		all[name] = this;
	}

	play { |...args|
		var env = Environment.newFrom(args), evPlayer;
		if (env[\player].isNil, { env.put(\player, DmxPlayer.default); });
		if (env[\fixtures].isNil, {
			var group = env[\group];
			if (group.isKindOf(Symbol), {
				group = DmxPatcher.default.groups[group];
			});
			env.put(\fixtures, group.fixtures);
		});
		evPlayer = env[\player].play(func.valueWithEnvir(env));
		players.add(evPlayer);
	}

	stop { |player = \default|
		DmxPlayer.all[player].stop(players);
		players.clear;
	}

	isPlaying {
		^(players.size > 0);
	}
}