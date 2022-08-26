
/*
	Provides methods to use devices without knowing their exact addresses...
	holds it's own little buffer for it's own dmx data, accessible via set, info gets called from patcher
 */
DmxFixture {
	classvar types; // holds different types of devices
	var <buffer;
	var <>address = 0;
	var <type;
	var <buses;
	var <routine;
	var <matrix;
	var <busvals;
	var <colors;

	*loadLibrary { |name|
		(Quark("DMXQuark").localPath ++ "/Fixtures/%.scd").format(name).load;
		"% fixture library loaded".format(name).postln;
	}

	*addType { |title, definition|
		if(types.isNil, {
			types = IdentityDictionary();
		});
		if(title.isKindOf(Symbol).not, {
			"Give a symbol as fixture title!".postln;
			^false;
		});
		if(definition.isKindOf(Event).not, {
			"Give an event as definition...".postln;
			^false;
		});
		if (definition[\channels].notNil, {
			this.addActions(definition);
		});
		definition[\numChannels] = definition[\numChannels] ? 1;
		types.put(title.asSymbol, definition);
	}

	*addActions { |definition|
		var channels = definition[\channels];
		// todo: allow overrides
		// color
		if (channels.indexOf(\r).notNil and: {channels.indexOf(\g).notNil and: {channels.indexOf(\b).notNil} }, {
			definition[\color] = { |self, color|
				var rgb = [0, 0, 0];
				if (color.isKindOf(Symbol), {
					color = Color.newName(color, nil, true);
				});
				rgb = color.asArray;
				self.set(\r, (rgb[0] * 255).round.asInteger);
				self.set(\g, (rgb[1] * 255).round.asInteger);
				self.set(\b, (rgb[2] * 255).round.asInteger);
				definition[\getColor] = { |self|
					var idx = self.channel(\r);
					self.getData(idx, idx+2);
				};
			};
		}, {
			if (channels.indexOf(\c).notNil and: {channels.indexOf(\m).notNil and: {channels.indexOf(\y).notNil} }, {
				definition[\color] = { |self, color|
					var cmy = [0, 0, 0];
					if (color.isKindOf(Symbol), {
						color = Color.newName(color, nil, true);
					});
					if (color.isKindOf(Color), {
						cmy[0] = 1.0 - color.red;
						cmy[1] = 1.0 - color.green;
						cmy[2] = 1.0 - color.blue;
					}, {
						cmy = color;
					});
					self.set(\c, (cmy[0] * 255).round.asInteger);
					self.set(\m, (cmy[1] * 255).round.asInteger);
					self.set(\y, (cmy[2] * 255).round.asInteger);
				};
				definition[\getColor] = { |self|
					var idx = self.channel(\c);
					self.getData(idx, idx+2);
				};
			});
		});
		// pan
		if (channels.indexOf(\panc).notNil and: {channels.indexOf(\panf).notNil}, {
			definition[\pan] = { |self, pan|
				self.set(\panc, (pan * 255).floor.asInteger);
				self.set(\panf, ((pan % (1/255)) * 255 * 255).round.asInteger);
			};
		});
		if (channels.indexOf(\pan).notNil or: { definition[\pan].notNil }, {
			definition[\panDeg] = { |self, degrees|
				self.set(\pan, (degrees / 540.0).min(1.0));
			};
			definition[\panCenter] = { |self, angle|
				self.set(\panDeg, (270 + angle).min(315).max(225));
			};
			definition[\panCross] = { |self, angle|
				self.set(\panDeg, (225 + angle).min(270).max(180));
			};
			definition[\panSides] = { |self, angle|
				self.set(\panDeg, (315 + angle).min(360).max(270));
			};
		});
		// tilt
		if (channels.indexOf(\tiltc).notNil and: { channels.indexOf(\tiltf).notNil }, {
			definition[\tilt] = { |self, tilt|
				self.set(\tiltc, (tilt * 255).floor.asInteger);
				self.set(\tiltf, ((tilt % (1/255)) * 255 * 255).round.asInteger);
			};
		});
		if (channels.indexOf(\tilt).notNil or: { definition[\tilt].notNil }, {
			definition[\tiltDeg] = { |self, degrees|
				self.set(\tilt, (degrees / 270.0).min(1.0));
			};
			definition[\tiltSky] = { |self, angle|
				self.set(\tiltDeg, (270 + angle).min(270).max(225));
			};
			definition[\tiltAudience] = { |self, angle|
				self.set(\tiltDeg, (180 + angle).min(225).max(135));
			};
			definition[\tiltDown] = { |self, angle|
				self.set(\tiltDeg, (135 + angle).min(180).max(90));
			};
			definition[\tiltFront] = { |self, angle|
				self.set(\tiltDeg, (225 + angle).min(270).max(180));
			};
		});
		// pos
		if (definition[\pan].notNil and: { definition[\tilt].notNil }, {
			definition[\pos] = { |self, pos|
				self.set(\pan, pos[0]);
				self.set(\tilt, pos[1]);
			};
		});
		// shutter
		if (channels.indexOf(\shutter).notNil, {
			definition[\strobe] = { |self, speed|
				self.set(\shutter, \strobe, speed);
			};
			definition[\pulse] = { |self, speed|
				self.set(\shutter, \pulse, speed);
			};
			definition[\strobeRand] = { |self, speed|
				self.set(\shutter, \strobe_rand, speed);
			};
		});
	}

	*types {
		if(types.isNil, {
			types = IdentityDictionary();
		});
		^types;
	}

	*typeNames {
		var myTypes = [];
		types.keysValuesDo({|name, dev|
			myTypes = myTypes.add(name);
		});
		^myTypes;
	}

	*createRange { |names|
		var range = ();
		var size = (256 / names.size).asInteger;
		names.do { |name, i|
			var from = i * size;
			var to = ((i+1) * size - 1).min(255);
			range.add(name.asSymbol -> (from..to));
		};
		^range;
	}

	*busesForMethod { |method, fixtureList|
		var buses = List();
		fixtureList.do({ |fixture, i|
			if(fixture.hasMethod(method), {
				buses.add(fixture.buses[method]);
			});
		});
		^buses;
	}

	*numBusesForMethod { |method, fixtureList|
		var numbuses = 0;
		fixtureList.do({ |fixture, i|
			if(fixture.hasMethod(method), {
				numbuses = numbuses +1;
			});
		});
		^numbuses;
	}

	*new { | mytype, mybuffer, myaddress = 1 |
		^super.new.init(mytype, mybuffer, myaddress);
	}

	init { | mytype, mybuffer, myaddress = 1 |
		if (myaddress < 1, {
			"DMX addresses start from 1".format(mytype).throw;
		});
		if (types[mytype].isNil, {
			"fixture type not found %s".format(mytype).throw;
		});
		type = mytype;
		buffer = mybuffer;
		address = myaddress;
		matrix = Array.fill(this.numChannels, 1);
		busvals = ();
		colors = List();
	}

	makeBuses { |server|
		var reservedKeys = ['numChannels', 'channels', 'init', 'buses'];
		buses = Dictionary();
		DmxFixture.types[type].keysValuesDo({ |method|
			// do for each method, but omit reserved keys 'channel', 'init', 'buses:
			if(reservedKeys.includes(method) == false, {
				var numArgs = DmxFixture.types[type].buses[method];
				if (numArgs.notNil, {
					var bus = Bus.control(server ? Server.default, numArgs);
					bus.setSynchronous(-1);
					buses.put(method, bus);
				});
			});
		});
		^buses;
	}

	freeBuses {
		buses.do({ |bus|
			bus.free;
		});
	}

	makeRoutine { |fps|
		routine = Routine.run({
			inf.do({
				this.updateFromBuses;
				(1/fps).wait;
			});
		});
		^routine;
	}

	updateFromBuses {
		var val, changed;
		buses.keysValuesDo({ |method, bus|
			var numArgs = DmxFixture.types[type].buses[method];
			if (numArgs > 1, {
				val = bus.getnSynchronous;
				changed = val[0] > 0 and: { val != busvals[method] };
			}, {
				val = bus.getSynchronous;
				changed = val > 0 and: { val != busvals[method] };
			});
			if (changed, {
				this.set(method, val);
			});
			busvals[method] = val;
		});
	}

	stop {
		routine.stop;
		this.freeBuses();
	}

	set { |arg1, arg2, arg3|
		if (arg1.isKindOf(Symbol) and: { this.hasMethod(arg1) }, {
			this.action(arg1, arg2);
		}, {
			var values = arg1, chan = arg2 ? 0;
			var index, to;
			if (arg1.isKindOf(Symbol) and: { arg2.isKindOf(Symbol) or: { arg2.isKindOf(Association) } }, {
				if (this.hasRange(arg1, arg2), {
					arg2 = this.range(arg1, arg2, arg3 ? 0);
				}, {
					"range % not found in % for channel %".format(arg2, type.asString, arg1).postln;
				});
			});
			if (arg1.isKindOf(SequenceableCollection).not, {
				values = [arg2];
				chan = arg1;
			});
			index = chan;
			if (chan.isKindOf(Symbol), {
				index = this.channels.indexOf(chan);
			});
			if (index.notNil, {
				if (values.size > this.numChannels, {
					values = values[0..(this.numChannels-1)];
					"more values than channels passed".postln;
				});
				to = index + values.size - 1;
				buffer.set(values * matrix[index..to], (address-1) + index);
			}, {
				"channel % not found in %".format(chan, type).postln;
			});
		});
	}

	trySet { |arg1, arg2, arg3|
		var chan = arg2;
		if (arg1.isKindOf(SequenceableCollection).not, {
			chan = arg1;
		});
		if (chan.isNumber or: { this.hasChannel(chan) or: { this.hasMethod(chan) } }, {
			this.set(arg1, arg2, arg3);
		});
	}

	get { |chan|
		if (chan.isKindOf(Symbol), {
			chan = this.channels.indexOf(chan);
			if (chan.isNil, {
				"channel % not found in %".format(chan, type).throw;
			});
		});
		^(buffer.buffer[(address-1)+chan] * matrix[chan]);
	}

	getData { |from = 0, to = -1|
		while ({ to < 0 }, {
			to = to + this.numChannels;
		});
		^(buffer.buffer[(address-1+from)..(address-1+to)] * matrix[from..to]);
	}

	action { |method, arguments|
		var def = types[type].at(method.asSymbol);
		if (def.notNil, {
			^def.value(this, arguments);
		}, {
			"method % not found in %".format(method, type.asString).postln;
		});
	}

	tryAction {
		if(this.hasMethod(\init), {
			this.action(\init);
		});
	}

	hasMethod { |method|
		^types[type].at(method.asSymbol).notNil;
	}

	numChannels {
		^types[type][\numChannels];
	}

	channels {
		^types[type][\channels];
	}

	channel { |name|
		var index = this.channels.indexOf(name);
		if (index.notNil, {
			^index;
		}, {
			"channel % not found in %!".format(name, type.asString).postln;
		});
	}

	hasChannel { |name|
		^this.channels.indexOf(name).notNil;
	}

	ranges {
		^types[type][\ranges];
	}

	range { |channel, name, value|
		var chRange = if (this.ranges.notNil, { this.ranges.at(channel) }, { nil });
		if (chRange.notNil, {
			var range = chRange.at(if (name.isKindOf(Association), { name.key }, { name }));
			if (range.notNil, {
				if (value.notNil, {
					^range[((range.size-1) * value).round.asInteger];
				}, {
					^range;
				});
			}, {
				"range % not found in % for channel %".format(name, type.asString, channel).postln;
			});
		}, {
			"% ranges not defined for channel %".format(type.asString, channel).postln;
		});
	}

	hasRange { |channel, range|
		var key = if (range.isKindOf(Association), { range.key }, { range });
		^(this.ranges.notNil and: { this.ranges.at(channel).notNil and: { this.ranges.at(channel).at(key).notNil } });
	}

	multiplier { |chan|
		if (chan.isKindOf(Symbol), {
			chan = this.channel(chan);
		});
		^matrix[chan];
	}

	invertChannel { |chans|
		if (chans.isKindOf(SequenceableCollection).not, {
			chans = [chans];
		});
		chans.do { |chan|
			if (chan.isKindOf(Symbol), {
				var index = this.channels.indexOf(chan);
				if (index.isNil, {
					"channel % not found in %".format(chan, type).throw;
				});
				chan = index;
			});
			matrix[chan] = matrix[chan] * -1;
		};
	}

	isInverted { |chan|
		^(this.multiplier(chan) == -1);
	}

	pushColor { |color|
		if (this.hasMethod(\getColor), {
			colors.add(this.action(\getColor));
			this.action(\color, color);
		}, {
			"can't pushColor: % does not implement 'getColor' action".format(type).postln;
		});
	}

	popColor {
		if (colors.size > 0, {
			var last = colors.last;
			this.action(\color, last);
			colors.remove(last);
		});

	}
}
