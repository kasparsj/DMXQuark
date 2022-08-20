DmxSoundIn {
	var <in;
	var <threshBus;
	var <rate;
	var <synth;
	var <onset;
	var <amp;
	var <loudness;
	var <mfcc;
	var <onData;
	var <onOnset;
	classvar <all;

	*initClass {
		Class.initClassTree(SynthDef, OSCdef);

		SynthDef(\dmx_soundin, {|out=0, in=0, thresh=0.5, rate=60|
			var input, amp, chain, onset, loudness, mfcc, trig;
			input = SoundIn.ar(in);
			chain = FFT(LocalBuf(1024), input);
			onset = Onsets.kr(chain, thresh);
			amp = Amplitude.kr(input);
			loudness = Loudness.kr(chain);
			mfcc = MFCC.kr(chain, 3);
			trig = Impulse.kr(rate);
			SendReply.kr(trig, '/dmx_soundin', [in, onset, amp, loudness] ++ mfcc);
		}).add;

		OSCdef(\dmx_soundin, {|msg|
			var data = msg[3..];
			var in = data[0].asInteger;
			var instance = DmxSoundIn.all[in];
			instance.prSoundData(data);
		}, '/dmx_soundin');

		all = ();
	}

	*new { |myIn = 0, myThresh = 0.5, myRate = 60|
		^super.new.init(myIn, myThresh, myRate);
	}

	init { |myIn = 0, myThresh = 0.5, myRate = 60|
		this.end;
		in = myIn;
		threshBus = Bus.control(Server.default).set(myThresh);
		rate = myRate;
		synth = Synth(\dmx_soundin, [in: in, thresh: threshBus.asMap, rate: myRate]);
		onData = DmxSoundIn_onData();
		onOnset = DmxSoundIn_onOnset();
		all.add(in -> this);
	}

	thresh {
		threshBus.getSynchronous;
	}

	thresh_ { |value|
		threshBus.set(value);
	}

	end {
		if (synth.notNil, {
			synth.free;
		});
		threshBus.free;
		all.removeAt(in);
	}

	prSoundData { |data|
		onset = data[1].asInteger;
		amp = data[2];
		loudness = data[3];
		mfcc = data[4..];
		onData.prSoundIn(this);
		onOnset.prSoundIn(this);
	}
}

DmxSoundIn_onData {
	var <handlers;

	*new {
		^super.new.init;
	}

	init {
		handlers = List();
	}

	add { |handler|
		handlers.add(handler);
	}

	remove { |handler|
		handlers.remove(handler);
	}

	clear {
		handlers.clear;
	}

	prSoundIn { |soundIn|
		handlers.do { |handler|
			handler.value(soundIn);
		};
	}
}

DmxSoundIn_onOnset {
	var <onHandlers;
	var <offHandlers;
	var <onceHandlers;
	var <wasOnset;

	*new {
		^super.new.init;
	}

	init {
		onHandlers = List();
		offHandlers = List();
		onceHandlers = List();
		wasOnset = false;
	}

	add { |onHandler, offHandler, delay|
		onHandlers.add(onHandler);
		if (offHandler.notNil, {
			offHandlers.add([offHandler, delay ? 0]);
		});
	}

	remove { |onHandler, offHandler|
		onHandlers.remove(onHandler);
		if (offHandler.notNil, {
			var idx = offHandlers.collect(_.first).indexOf(offHandler);
			offHandlers.removeAt(idx);
		});
	}

	doOnce { |handler|
		onceHandlers.add(handler);
	}

	clear {
		onHandlers.clear;
		offHandlers.clear;
		onceHandlers.clear;
	}

	prSoundIn { |soundIn|
		if (soundIn.onset == 1 and: { wasOnset.not }, {
			onHandlers.do { |handler|
				handler.value(soundIn);
			};
			onceHandlers.do { |handler|
				handler.value(soundIn);
			};
			onceHandlers.clear;
			wasOnset = true;
		}, {
			if (wasOnset, {
				offHandlers.do { |handler|
					SystemClock.sched(handler[1], {
						handler[0].value(soundIn);
						nil;
					});
				};
				wasOnset = false;
			});
		});
	}
}