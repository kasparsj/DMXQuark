// eDMX1 Pro on NetAddr(192.168.1.102);
// strobes - cameo thunderwash on 400, 500 (7 chans)
// pars - adj mega hex pars on 100, 108, 116, 124 (8 chans)

(
~buf = DmxBuffer();
~buf.addDevice(OlaOsc());

// load rainstar fixture library
DmxFixture.loadLibrary("adj");
DmxFixture.loadLibrary("cameo");

// create a project
~dmx = DmxPatcher(\insterburg);
~dmx.addFixture(DmxFixture(\adj_mega_hex_par_8, ~buf, 100), [\pars]);
~dmx.addFixture(DmxFixture(\adj_mega_hex_par_8, ~buf, 108), [\pars]);
~dmx.addFixture(DmxFixture(\adj_mega_hex_par_8, ~buf, 116), [\pars]);
~dmx.addFixture(DmxFixture(\adj_mega_hex_par_8, ~buf, 124), [\pars]);

~dmx.addFixture(DmxFixture(\cameo_thunderwash_600_7_1, ~buf, 500), [\wash]);
~dmx.addFixture(DmxFixture(\cameo_thunderwash_600_7_1, ~buf, 400), [\wash]);

// create a player
~player = DmxPlayer.default;

~soundIn = DmxSoundIn(0, 0.7, 60);

~blueColors = Color.xwindows.keys.select({ |key| key.asString.find("blue").notNil });
~greenColors = Color.xwindows.keys.select({ |key| key.asString.find("green").notNil });
~redColors = Color.xwindows.keys.select({ |key| key.asString.find("red").notNil });
~orangeColors = Color.xwindows.keys.select({ |key| key.asString.find("orange").notNil });
~yellowColors = Color.xwindows.keys.select({ |key| key.asString.find("yellow").notNil });
~pinkColors = Color.xwindows.keys.select({ |key| key.asString.find("pink").notNil });
~magentaColors = Color.xwindows.keys.select({ |key| key.asString.find("magenta").notNil });
)

// turn washes off
PdmxScene(\group, \wash, \dimmer, 0).play;

// turn washes almost off
PdmxScene(\group, \wash, \dimmer, 2, \color, [127, 127, 0]).play;

// magenta & cyan
PdmxScene(\group, \wash, \fixtures, 0, \dimmer, 255, \shutter, \open, \color, [0, 1.0, 1.0]).play;
PdmxScene(\group, \wash, \fixtures, 1, \dimmer, 255, \shutter, \open, \color, [1.0, 0, 1.0]).play;

// fade washes dimmer
(
z.stop;
z = {
	inf.do { |i|
		PdmxScene(\group, \wash, \dimmer, (i%20)).play;
		0.25.wait;
	};
}.fork;
)

(
// change color wheel every 5 seconds
y.stop;
y = {
	inf.do {
		PdmxScene(\group, \wash, \shutter, \ramp_up_rand).play;
		1.wait;
		PdmxScene(\group, \wash, \shutter, \closed).play;
		5.wait;
	}
}.fork;
)

// pars anim
(
x.stop;
x= {
	inf.do { |i|
		4.do { |j|
			PdmxScene(\group, \wash, \shutter, 0).play;
			~dmx.groups[\wash].fixtures[j].set(\color, [255, 0, 0]);
			~dmx.groups[\wash].fixtures[j].set(\shutter, 255);
			0.03.wait;
		};
	};
}.fork;
)

// set dimmer to full for 1 second (sudden flash of light when executed)
(
w.stop;
w = {
	PdmxScene(\group, \wash, \dimmer, 255).play;
	1.wait;
	PdmxScene(\group, \wash, \dimmer, 0).play;
}.fork;
)

// strobe on/off
(
v.stop;
v = {
	inf.do { |i|
		PdmxScene(\group, \wash, \shutter, 11+240.rand).play;
		15.rand.wait;
		PdmxScene(\group, \wash, \shutter, 0).play;
		15.rand.wait;
	};
}.fork;
)

(
u.stop;
u = {
	inf.do { |i|
		PdmxScene(\group, \wash, \dimmer, 10, \shutter, \open, \color, [255.rand, 0, 255.rand]).play;
		3.wait;
	};
}.fork;
)

// reset the whole buffer
512.do { |i|
	~buf.set(i, 0);
}

// reset pars
32.do { |i|
	~buf.set(99 + i, 0);
}

// check sound data is coming in
(
~soundIn.onData.clear;
~soundIn.onData.add({ |soundIn|
	soundIn.postln;
});
)