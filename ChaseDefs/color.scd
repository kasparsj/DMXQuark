(
DmxChaseDef(\color_sine, { |player, fixtures, phase = 0, dur = inf, speed = 1|
	if (speed.isKindOf(SequenceableCollection).not, {
		speed = Array.fill(3, speed);
	});
	Pmono(\dmx_sine3,
		\dur, Pseq([dur], 1),
		\out, DmxFixture.busesForMethod(\color).collect(_.index),
		\freq_0, speed[0],
		\freq_1, speed[1],
		\freq_2, speed[2],
		\speed, player.fadeSpeed.asMap,
		\phase, phase,
	);
});
)