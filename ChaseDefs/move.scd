(
DmxChaseDef(\move_circle, { |player, fixtures, radius = 0.05, center = #[0.5, 0.75], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_circle,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\pos, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\radius, radius,
		\cx, center[0],
		\cy, center[1],
		\phase, phase,
	)
});

// todo: better run 2 chases
DmxChaseDef(\move_noise, { |player, fixtures, panRange = #[0.3, 0.7], tiltRange = #[0.5, 1.0], dur = inf, speed = 1|
	Ppar([
		DmxChaseDef(\pan_noise).value(player, fixtures, panRange, dur, speed),
		DmxChaseDef(\tilt_noise).value(player, fixtures, tiltRange, dur, speed),
	]);
});

DmxChaseDef(\pan_sine, { |player, fixtures, range = #[0.4, 0.6], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_sine,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\pan, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\pan_saw, { |player, fixtures, range = #[0.45, 0.55], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_saw,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\pan, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\pan_tri, { |player, fixtures, range = #[0.45, 0.55], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_tri,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\pan, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\pan_noise, { |player, fixtures, range = #[0.3, 0.7], dur = inf, speed = 1|
	Pmono(\dmx_noise,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\pan, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
	)
});

DmxChaseDef(\tilt_sine, { |player, fixtures, range = #[0.75, 1.0], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_sine,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\tilt, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\tilt_saw, { |player, fixtures, range = #[0.75, 1.0], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_saw,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\tilt, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\tilt_tri, { |player, fixtures, range = #[0.75, 1.0], phase = 0, dur = inf, speed = 1|
	Pmono(\dmx_tri,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\tilt, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
		\phase, phase,
	);
});

DmxChaseDef(\tilt_noise, { |player, fixtures, range = #[0.5, 1.0], dur = inf, speed = 1|
	Pmono(\dmx_noise,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\tilt, fixtures).collect(_.index),
		\freq, speed,
		\speed, player.moveSpeed.asMap,
		\lo, range[0],
		\hi, range[1],
	);
});
)
