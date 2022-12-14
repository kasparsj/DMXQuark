// ADJ 4 moving heads & 3 pars example
// SETUP
(
~buf = DmxBuffer();
SerialPort.devices.do { |dev|
	var port = "/dev/tty.usbserial-EN092504";
	if (dev == port) {
		//~buf.addDevice(EnttecDMXUSBPro(dev));
	}
};
~buf.addDevice(OlaOsc());

~dmx = DmxPatcher(\adj_example);

DmxFixture.loadLibrary("adj");
// fixtures setup - server must be booted
Server.default.waitForBoot {
	var numHeads = 4;
	var numPars = 3;
	numHeads.do { |i|
		var type = DmxFixture.types[\adj_vizi_cmy300_26];
		var fixture = DmxFixture(\adj_vizi_cmy300_26, ~buf, ~dmx.nextFreeAddr(type[\numChannels]));
		var groups = [\heads];
		~dmx.addFixture(fixture, groups);
		// invert pan
		if ([2, 3].indexOf(i).notNil, {
			fixture.invertChannel([\panc, \panf]);
		});
	};
	numPars.do { |i|
		var type = DmxFixture.types[\adj_12p_hex_ip_13];
		var fixture = DmxFixture(\adj_12p_hex_ip_13, ~buf, ~dmx.nextFreeAddr(type[\numChannels]));
		var groups = [\pars];
		~dmx.addFixture(fixture, groups);
	};

	~soundIn = DmxSoundIn(0, 0.7, 60);

	"setup complete".postln;
};
)

// GUI
DmxGui.new;

// CHASES

PdmxChase(\move_noise, \group, \heads, \speed, 0.1).play;
PdmxChase(\move_circle, \group, \heads, \speed, 0.1).play;
PdmxChase(\tilt_sine, \group, \heads, \speed, 0.1, \phase, Array.fill(~dmx.groups[\heads].size, { |i| 2pi/~dmx.groups[\heads].size*i })).play;
PdmxChase(\tilt_saw, \group, \heads, \speed, 0.1).play;
PdmxChase(\tilt_noise, \group, \heads, \speed, 0.1).play;
PdmxChase(\pan_sine, \group, \heads, \speed, 0.1, \phase, Array.fill(~dmx.groups[\heads].size, { |i| 2pi/~dmx.groups[\heads].size*i })).play;
PdmxChase(\pan_saw, \group, \heads, \speed, 0.1).play;
PdmxChase(\pan_noise, \group, \heads, \speed, 0.1).play;

// stop all chases
DmxPlayer.default.stopAll;

// SCENES

// pan
PdmxScene(\group, \heads, \panCenter, 0).play;
PdmxScene(\group, \heads, \panCross, 0).play;
PdmxScene(\group, \heads, \panSides, 0).play;
PdmxScene(\group, \heads, \panDeg, rrand(180, 360)).play;

// tilt
PdmxScene(\group, \heads, \tiltAudience, 20).play;
PdmxScene(\group, \heads, \tiltDown, 20).play;
PdmxScene(\group, \heads, \tiltFront, 0).play;
PdmxScene(\group, \heads, \tiltDeg, rrand(135, 270)).play;

// focus and zoom
PdmxScene(\group, \heads, \zoom, 0).play;
PdmxScene(\group, \heads, \zoom, 127).play;
PdmxScene(\group, \heads, \focus, 0).play;
PdmxScene(\group, \heads, \focus, 127).play;

// color wheel
PdmxScene(\group, \heads, \color_wheel, \open).play;
PdmxScene(\group, \heads, \color_wheel, \red).play;
PdmxScene(\group, \heads, \color_wheel, \cw_scroll->1.0).play;
PdmxScene(\group, \heads, \color_wheel, \ccw_scroll->1.0).play;

// color
PdmxScene(\group, \heads, \color, \white).play;
PdmxScene(\group, \heads, \color, \red).play;
PdmxScene(\group, \heads, \color, \magenta).play;

// color macro
PdmxScene(\group, \heads, \color_macro, \off).play;
PdmxScene(\group, \heads, \color_macro, \macro1).play;
PdmxScene(\group, \heads, \color_macro, \random_cmy).play;

// move & fade speed
PdmxScene(\group, \heads, \move_speed, 255).play;
PdmxScene(\group, \heads, \color_speed, 255).play;

// turn off
PdmxScene(\group, \heads, \shutter, \closed).play;
PdmxScene(\group, \pars, \shutter, \closed).play;

// Pbind SCENES
Pdef(\randomPan, Pbind(\type, \dmx, \group, \heads, \panDeg, Pwhite(180, 360))).play;
Pdef(\randomPan).stop;

// BEAT
// todo: comment
(
~soundIn.onData.clear;
~soundIn.onData.add({ |soundIn|
	var mul = 100;
	PdmxScene(\group, \pars, \dummer, ((soundIn.amp * mul) * 255).round.asInteger.max(255)).play;
});
)

(
~soundIn.onData.clear;
~soundIn.onData.add({ |soundIn|
	PdmxScene(\group, \pars, \color, soundIn.mfcc);
});
)

(
~soundIn.onOnset.clear;
~soundIn.onOnset.add({ |soundIn|
	// PdmxScene(\group, \heads, \panDeg, rrand(180, 360));

	// not good: strobe stays on
	// PdmxScene(\group, \heads, \shutter, 256.rand);
	PdmxScene(\group, \heads, \fixture, (0..3).choose,  \shutter, \open);
}, {
	PdmxScene(\group, \heads, \shutter, \closed);
}, 0.1);
)

// DEBUG & TESTING

// inspect buffer
~buf.buffer

// modify the buffer directly
~buf.set(0, 255.rand); // pan coarse
~buf.set(1, 123); // pan fine
~buf.set(2, 255.rand); // tilt coarse
~buf.set(3, 123); // tilt fine
~buf.set(5, 255); // cyan
~buf.set(17, 95); // shutter
~buf.set(18, 255); // dimmer