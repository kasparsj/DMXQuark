(
DmxFixture.addType(\rainstar_120_led_sharpy_beam_21, (
	numChannels: 21,
	channels: [
		\panc, \panf, \tiltc, \tiltf, // 0 - 3
		\dimmer, // 4
		\strobe, // 5
		\color_wheel, // 6
		\gobo, // 7
		\prism1, // 8
		\prism1_rotation, // 9
		\prism2, // 10
		\prism2_rotation, // 11
		\focus, \frost, // 12 - 13
		\move_speed, // 14
		\reset, // 15
		\dimmer, // 16
		\ring_strobe, // 17
		\r, \g, \b // 18 - 20
	],
	ranges: (
		strobe: (
			open: (0..5),
			closed: (6..10),
			plus: (11..33),
			ramp_up: (34..56),
			ramp_down: (57..79),
			random: (80..102),
			break: (103..127),
			slow_fast: (128..250),
			open2: (251..255),
		),
		// color_wheel: (
		// ),
		// gobo: (
		// ),
	),
	buses: (pos: 2, pan: 1, tilt: 1, color: 3), // busName -> numArgs
	init: { |self|
		// pan/tilt center
		self.set([127, 127, 127, 127], 0);
		// pan/tilt speed fast
		self.set(\move_speed, 0);
		// dimmer full:
		self.set(\dimmer, 255);
	}
));
)
