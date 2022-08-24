(
DmxFixture.addType(\adj_vizi_cmy300_26, (
	numChannels: 26,
	channels: [
		\panc, \panf, \tiltc, \tiltf, // 0 - 3
		\color_wheel, // 4
		\c, \m, \y, // 5 - 7
		\gobo1, \gobo_rotation, \gobo2, // 8 - 10
		\prism1, \prism1_rotation, \prism2, \prism2_rotation, // 11 - 14
		\focus, \zoom, \shutter, // 15 - 17
		\dimmer, // 18
		\iris, \frost, // 19 - 20
		\color_speed, \move_speed, // 21 - 22
		\color_macro, \dimmer_mode, // 23 - 24
	],
	ranges: (
		color_wheel: (
			open: (0..6),
			red: (7..12),
			light_green: (13..18),
			pink: (19..25),
			orange: (26..31),
			ctb: (32..37), // confetti
			cto: (38..44),
			uv: (45..50),
			green: (41..56),
			light_blue: (57..63),
			color_indexing: (64..127),
			cw_scroll: (128..190),
			ccw_scroll: (193..255),
		),
		gobo1: (
			open: (0..7),
			gobo1: (8..15),
			gobo2: (16..23),
			gobo3: (24..31),
			gobo4: (32..39),
			gobo5: (40..47),
			gobo6: (48..55),
			gobo7: (56..63),
			gobo_shake1: (64..72),
			gobo_shake2: (73..81),
			gobo_shake3: (82..90),
			gobo_shake4: (91..99),
			gobo_shake5: (100..108),
			gobo_shake6: (109..117),
			gobo_shake7: (118..127),
			cw_scroll: (128..190),
			stop: (191..192),
			ccw_scroll: (193..255),
		),
		iris: (
			max_min: (0..191),
			pulse_closing: (192..223),
			pulse_opening: (224..255),
		),
		shutter: DmxFixture.createRange([\closed, \open1, \strobe, \open2, \pulse, \open3, \strobe_rand, \open]),
		color_macro: DmxFixture.createRange([\off] ++ (1..30).collect({ |i| "macro" ++ i }) ++ [\random_cmy]),
	),
	buses: (pos: 2, pan: 1, tilt: 1, color: 3), // busName -> numArgs
	init: { |self|
		// pan/tilt center
		self.set([127, 127, 127, 127], 0);
		// pan/tilt speed fast
		self.set(\move_speed, 0);
		// color wheel speed fast
		self.set(\color_speed, 0);
		// shutter open/dimmer full:
		self.set(\shutter, \open);
		self.set(\dimmer, 255);
	}
));

DmxFixture.addType(\adj_12p_hex_ip_13, (
	numChannels: 14,
	channels: [
		\r, \g, \b, \white, \amber, \uv,
		\dimmer,
		\shutter,
		\color_macro,
		\auto_prog,
		\auto_prog_speed,
		\auto_prog_fade,
		\dimmer_mode,
	],
	ranges: (
		shutter: DmxFixture.createRange([\closed, \open1, \strobe, \open2, \pulse, \open3, \strobe_rand, \open]),
		color_macro: DmxFixture.createRange([\off] ++ (1..64).collect({ |i| "color" ++ i })),
		auto_prog: DmxFixture.createRange([\none] ++ (1..12).collect({ |i| "program" ++ i })),
		dimmer_mode: (
			standard: (0..20),
			stage: (21..40),
			tv: (41..60),
			architectural: (61..80),
			theatre: (81..100),
			default_to_unit: (101..255),
		),
	),
	buses: (color: 3), // busName -> numArgs
	init: { |self|
		self.action(\color, \red);
		self.set(\dimmer, 255);
		self.set(\shutter, \open);
	},
));
)
