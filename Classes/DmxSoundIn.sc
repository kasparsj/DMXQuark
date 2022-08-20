DmxSoundIn {
	var <in;
	var <threshBus;
	var <rate;
	var <synth;
	var <onset;
	var <amp;
	var <loudness;
	var <mfcc;
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
			mfcc = MFCC.kr(chain);
			trig = Impulse.kr(rate);
			SendReply.kr(trig, '/dmx_soundin', [in, onset, amp, loudness] ++ mfcc);
		}).add;

		OSCdef(\dmx_soundin, {|msg|
			var data = msg[3..];
			var in = data[0].asInteger;
			var instance = DmxSoundIn.all[in];
			instance.onSoundData(data);
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

	onSoundData { |data|
		onset = data[1].asInteger;
		amp = data[2];
		loudness = data[3];
		mfcc = data[4..];
		if (onset == 1, {
			this.onOnset;
		});
	}

	onOnset {

	}
}