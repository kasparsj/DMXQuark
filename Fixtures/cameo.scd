(
DmxFixture.addType(\cameo_thunderwash_600_7_1, (
	numChannels: 7,
	channels: [
		\dimmer,
		\shutter,
		\r, \g, \b, \w,
		\sound,
	],
	ranges: (
		shutter: (
			open1: (0..5),
			closed: (6..10),
			pulse_rand: (11..33),
			ramp_up_rand: (34..56),
			ramp_down_rand: (57..79),
			rand: (80..102),
			break: (103..127),
			speed: (128..250),
			open: (251..255),
		)
	),
	buses: (color: 3), // busName -> numArgs
	init: { |self|
		self.action(\color, \red);
		self.set(\dimmer, 255);
		self.set(\shutter, \open);
	},
));
)