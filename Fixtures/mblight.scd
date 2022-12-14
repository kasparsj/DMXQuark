(
//  numChannels - holds to total number of dmx channels used by this fixture
//  buses - holds number of arguments each method expects
//  init - holds the init method which i.e. can set some default values
DmxFixture.addType(\dim, (
	numChannels: 1,
	buses: (dim: 1),
	dim: { |self, args|
		self.set(0, (args[0] * 255).round.asInteger);
	},
	init: { |self|
		self.set(0, 0);
	}
));
DmxFixture.addType(\camera, (
	numChannels: 16,
	buses: (camerapos: 3, cameraaim: 3),
	camerapos: { |self, args|
		// set pos (x, y, z) and direction (pan, title – no pitch for now) if wanted
		// split x, y, z to msb/lsb (coarse/fine)
		var x = [(args[0] * 255).floor, (args[0] * 255 % 1 * 255).round];
		var y = [(args[1] * 255).floor, (args[1] * 255 % 1 * 255).round];
		var z = [(args[2] * 255).floor, (args[2] * 255 % 1 * 255).round];

		self.set(0, x[0]);
		self.set(1, x[1]);
		self.set(2, y[0]);
		self.set(3, y[1]);
		self.set(4, z[0]);
		self.set(5, z[1]);
	},
	cameraaim: { |self, args|
		// set pan/tilt to where the camera should look to...
		// args: aimx, aimy, aimz
		var pans, tilts;

		// get current camera position:
		var dmx = self.getData();
		var xcam = dmx[0] / 255 + (dmx[1] / 255 / 255);
		var ycam = dmx[2] / 255 + (dmx[3] / 255 / 255);
		var zcam = dmx[4] / 255 + (dmx[5] / 255 / 255);

		// calculate stuff.
		// 1. distance on x/z-plane using pythagoras
		var d = ((xcam - args[0])**2 + ((zcam - args[2])**2)).sqrt;
		// 2. "height"-difference (delta y)
		var h = ycam - args[1];
		// 3. tilt from 1. and 2.
		var tilt = (h / d).atan;
		// 4. pan: delta x / delta z...
		// but counter-clockwise!?
		var dx = xcam - args[0];
		var dz = zcam - args[2];
		var pan = (dx / dz).atan * -1;

		if(dz < 0, {
			pan = pan + pi;
		});

		pan = pan.wrap(0, 2pi);
		tilt = tilt.wrap(0, 2pi);

		tilts = [(tilt/2pi * 255).floor, (tilt/2pi * 255 % 1 * 255).round];
		pans = [(pan/2pi * 255).floor, (pan/2pi * 255 % 1 * 255).round];

		self.set(6, pans[0]);
		self.set(7, pans[1]);
		self.set(8, tilts[0]);
		self.set(9, tilts[1]);
	},
	init: { |self|
		self.set(0, 128);
		self.set(1, 0);
		self.set(2, 129);
		self.set(3, 0);
		self.set(4, 140);
		self.set(5, 0);

		self.set(13, 20); // ambient light
		self.set(14, 128); // overall light?? alpha-value??
		self.set(15, 40); // "athmosphere" -> foggyness
	}
));
DmxFixture.addType(\smplrgb, (
	numChannels: 3,
	buses: (color: 3),
	color: { |self, args|
		self.set(0, (args[0] * 255).round.asInteger);
		self.set(1, (args[1] * 255).round.asInteger);
		self.set(2, (args[2] * 255).round.asInteger);
	}
));
DmxFixture.addType(\waldpar, (
	numChannels: 6,
	buses: (color: 3, strobe: 1),
	color: { |self, args|
		self.set(0, (args[0] * 255).round.asInteger);
		self.set(1, (args[1] * 255).round.asInteger);
		self.set(2, (args[2] * 255).round.asInteger);
	},
	init: { |self|
		self.set(3, 0); // color-shifter??
		self.set(4, 0); // shutter
		self.set(5, 0); // macro
	},
	strobe: { |self, args|
		self.set(4, (args[0] * 255).round.asInteger);
	}
));
DmxFixture.addType(\waldfuck2, (
	numChannels: 7,
	// dim, strobe, r, g, b, w, chaser, chaser2
	buses: (color: 3),
	color: { |self, args|
		self.set(2, (args[0] * 255).round.asInteger);
		self.set(3, (args[1] * 255).round.asInteger);
		self.set(4, (args[2] * 255).round.asInteger);
	},
	init: { |self|
		self.set(0, 255); // dimmer
		self.set(1, 0); // strobe
		self.set(5, 0); // w
		self.set(6, 0); // chaser
	}
));
DmxFixture.addType(\waldfuck, (
	numChannels: 6,
	buses: (color: 3),
	color: { |self, args|
		self.set(1, (args[0] * 255).round.asInteger);
		self.set(2, (args[1] * 255).round.asInteger);
		self.set(3, (args[2] * 255).round.asInteger);
	},
	init: { |self|
		self.set(0, 255); // dimmer
		self.set(4, 0); // macro
		self.set(5, 0); // strobe
	}
));
DmxFixture.addType(\waldbarInit, (
	numChannels: 11,
	buses: (),
	init: { |self|
		// set intensity of whole bar to FL
		self.set(10, 255);
	}
));
DmxFixture.addType(\waldbar, (
	numChannels: 3,
	buses: (color: 3),
	color: { |self, args|
		self.set(0, (args[0] * 255).round.asInteger);
		self.set(1, (args[1] * 255).round.asInteger);
		self.set(2, (args[2] * 255).round.asInteger);
	}
));
DmxFixture.addType(\waldfog, (
	numChannels: 1,
	buses: (fog: 1),
	fog: { |self, args|
		self.set(0, (args[0]*255).round.asInteger);
	},
	init: {|self| self.set(0, 0) }
));
DmxFixture.addType(\waldblitz, (
	numChannels: 2,
	buses: (blitz: 1),
	blitz: { |self, args|
		self.set(0, (args[0] * 255).round.asInteger);
	},
	init: { |self|
		self.set(0, 255);
		self.set(1, 255); // set intens to fl
	}
));
DmxFixture.addType(\robeCw1200E, (
	numChannels: 17,
	buses: (color: 3, cmyk: 4, strobe: 1, zoom: 1),
	init: { |self|
		// pan/tilt center:
		self.set(0, 127);
		self.set(2, 127);
		// shutter open:
		self.set(15, 255);
		// white/poweron/intensity:
		self.set(16, 255);
		// zoom: narrowest...
		self.set(14, 0);
	},
	color: { |self, rgb|
		// rgb 2 cmyk:
		var cmyk = [0, 0, 0, 0];
		rgb = rgb.clip(0, 1); // clip incoming...
		// another try: http://stackoverflow.com/questions/2426432/convert-rgb-color-to-cmyk
		//Black   = minimum(1-Red,1-Green,1-Blue)
		//Cyan    = (1-Red-Black)/(1-Black)
		//Magenta = (1-Green-Black)/(1-Black)
		//Yellow  = (1-Blue-Black)/(1-Black)
		cmyk[3] = (1.0-rgb).minItem;
		cmyk[0] = (1 - rgb[0] - cmyk[3]) / (1 - cmyk[3]);
		cmyk[1] = (1 - rgb[1] - cmyk[3]) / (1 - cmyk[3]);
		cmyk[2] = (1 - rgb[2] - cmyk[3]) / (1 - cmyk[3]);

		// set cmyk...
		self.set(8, (cmyk[0] * 255).round.asInteger);
		self.set(9, (cmyk[1] * 255).round.asInteger);
		self.set(10, (cmyk[2] * 255).round.asInteger);
		self.set(16, 255 - (cmyk[3] * 255).round.asInteger); // k is intensity 'inversed'
	},
	// careful! Don't write multiple actions that overwrite each other! Oh noes...
	/*			cmyk: { |self, cmyk|
	cmyk = (cmyk * 255).round.asInteger;
	self.set(8, cmyk[0]);
	self.set(9, cmyk[1]);
	self.set(10, cmyk[2]);
	self.set(16, 255 - cmyk[3]); // k is intensity 'inversed'
	},*/
	strobe: { |self, strobe|
		if(strobe[0] == 0, {
			self.set(15, 255);
		}, {
			self.set(15, (strobe[0] * 254).round.asInteger);
		});
	},
	zoom: { |self, zoom|
		self.set(14, (zoom[0] * 255).round.asInteger);
	}
));
DmxFixture.addType(\ClrChngr, (
	numChannels: 11,
	buses: (color: 3, cmyk: 4),
	init: { |self|
		// shutter open:
		self.set(4, 255);
		// white/poweron/intensity:
		self.set(5, 255);
		// zoom:
		self.set(7, 255);
	},
	color: { |self, rgb|
		// rgb 2 cmyk:
		var cmyk = [0, 0, 0, 0];
		rgb = rgb.clip(0, 1); // clip incoming...
		// another try: http://stackoverflow.com/questions/2426432/convert-rgb-color-to-cmyk
		//Black   = minimum(1-Red,1-Green,1-Blue)
		//Cyan    = (1-Red-Black)/(1-Black)
		//Magenta = (1-Green-Black)/(1-Black)
		//Yellow  = (1-Blue-Black)/(1-Black)
		cmyk[3] = (1.0-rgb).minItem;
		cmyk[0] = (1 - rgb[0] - cmyk[3]) / (1 - cmyk[3]);
		cmyk[1] = (1 - rgb[1] - cmyk[3]) / (1 - cmyk[3]);
		cmyk[2] = (1 - rgb[2] - cmyk[3]) / (1 - cmyk[3]);

		// set cmyk...
		self.set(1, (cmyk[0] * 255).round.asInteger);
		self.set(2, (cmyk[1] * 255).round.asInteger);
		self.set(3, (cmyk[2] * 255).round.asInteger);
		self.set(5, 255 - (cmyk[3] * 255).round.asInteger); // k is intensity 'inversed'
	},
	// careful! Don't write multiple actions that overwrite each other! Oh noes...
	/*			cmyk: { |self, cmyk|
	cmyk = (cmyk * 255).round.asInteger;
	self.set(8, cmyk[0]);
	self.set(9, cmyk[1]);
	self.set(10, cmyk[2]);
	self.set(16, 255 - cmyk[3]); // k is intensity 'inversed'
	},*/
));
DmxFixture.addType(\waldStudio, (
	numChannels: 7,
	buses: (color: 3, strobe: 1),
	init: { |self|
		self.set(6, 255); // dimmer
		fork{
			self.set(0, 255);
			self.set(1, 255);
			self.set(2, 255);
			1.wait;
			self.set(0, 0);
			self.set(1, 0);
			self.set(2, 0);
		};
		self.set(3, 0); // macrostuff
		self.set(4, 0);
		self.set(5, 0);
	},
	color: { |self, rgb|
		self.set(0, (rgb[0] * 255).round.asInteger);
		self.set(1, (rgb[1] * 255).round.asInteger);
		self.set(2, (rgb[2] * 255).round.asInteger);
	},
	strobe: { |self, strobe|

	}

));

DmxFixture.addType(\fiveSpot, (
	numChannels: 6,
	buses: (colorw: 4, strobe: 1),
	init: { |self|
		self.set(4, 255); // dimmer
	},
	colorw: { |self, rgbw|
		self.set(0, (rgbw[0] * 255).round.asInteger);
		self.set(1, (rgbw[1] * 255).round.asInteger);
		self.set(2, (rgbw[2] * 255).round.asInteger);
		self.set(3, (rgbw[3] * 255).round.asInteger);
	},
	strobe: { |self, strobe|
		if(strobe[0] > 0, {
			strobe[0] = strobe[0].linlin(0, 1, 11, 255).round.asInteger;
		});
		self.set(5, strobe[0]);
	}

));

DmxFixture.addType(\platWashZFXProBasicMode, (
	numChannels: 20,
	buses: (color: 3, pos: 2),
	init: { |self|
		// pan/tilt center
		self.set(0, 127); self.set(1, 127);
		self.set(2, 127); self.set(3, 127);
		// shutter open:
		self.set(8, 255);
		// white/poweron/intensity:
		self.set(9, 255);
		// zoom: narrowest...
		self.set(10, 0);
	},
	color: { |self, rgb|
		self.set(4, (rgb[0] * 255).round.asInteger);
		self.set(5, (rgb[1] * 255).round.asInteger);
		self.set(6, (rgb[2] * 255).round.asInteger);
	},
	pos: { |self, pos|
		// pos is rotation/pan and tilt in coarse and fine values
		self.set(0, (pos[0] * 255).floor.asInteger); // coarse
		self.set(1, ((pos[0] % (1/255)) * 255 * 255).round.asInteger); // fine

		self.set(2, (pos[1] * 255).floor.asInteger);
		self.set(3, ((pos[1] % (1/255)) * 255 * 255).round.asInteger);
	}
));

DmxFixture.addType(\smplrgbw, (
	numChannels: 4,
	buses: (color: 3),
	color: { |self, args|
		// rgbw: use white channel automatically...
		var white;
		// set white to the min value of all channels
		white = (args.minItem * 255).round.asInteger;
		self.set(3, white);
		// substract the white amount from the color channels
		self.set(0, (args[0] * 255).round.asInteger - white);
		self.set(1, (args[1] * 255).round.asInteger - white);
		self.set(2, (args[2] * 255).round.asInteger - white);
	}
));

// showtec led light bar 8, needs some initiating (dimmer and strobe channel...)
DmxFixture.addType(\showtecLLB8init, (
	numChannels: 26,
	buses: (),
	init: { |self|
		// strobe channel off
		self.set(24, 0);
		// dimmer channel full
		self.set(25, 255);
	}
));

DmxFixture.addType(\lmaxxeasywash, (
	numChannels: 12,
	buses: (color: 3, pos: 2),
	init: { |self|
		// pan/tilt center
		self.set(0, 127); self.set(1, 127);
		self.set(2, 127); self.set(3, 127);
		// pan/tilt speed fast
		self.set(4, 0);
		// shutter open/dimmer full:
		self.set(5, 255);
		// channels 10, 11, 12 are color, color speed and auto
		// funny how it has 13 channels in 12 channel mode... duh
	},
	color: { |self, rgb| // rgbw, channels 6 7 8 9
		// rgbw: use white channel automatically...
		var white;
		// set white to the min value of all channels
		white = (rgb.minItem * 255).round.asInteger;
		self.set(9, white);
		// substract the white amount from the color channels
		self.set(6, (rgb[0] * 255).round.asInteger - white);
		self.set(7, (rgb[1] * 255).round.asInteger - white);
		self.set(8, (rgb[2] * 255).round.asInteger - white);
	},
	pos: { |self, pos|
		// pos is rotation/pan and tilt in coarse and fine values
		self.set(0, (pos[0] * 255).floor.asInteger); // coarse
		self.set(1, ((pos[0] % (1/255)) * 255 * 255).round.asInteger); // fine

		self.set(2, (pos[1] * 255).floor.asInteger);
		self.set(3, ((pos[1] % (1/255)) * 255 * 255).round.asInteger);
	}
));
)