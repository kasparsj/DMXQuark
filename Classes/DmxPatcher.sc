/*
	DmxPatcher: Registers lighting equipment of certain types (?), manages routing to actual dmx addresses
 */
DmxPatcher {
	var <fixtures;
	var <groups;
	var <id;
	var <buffers; // holds DMXBuffer objects
	var <busses;
	var server; // holds default server since patcher uses busses!
	var <>fps; // fps to get data from busses with
	classvar <default; // default (usually first) DmxPatcher...
	classvar <all; // holds all opened patchers for reference...

	var aFun; // some vegas mode...

	*initClass {
		Class.initClassTree(Event);

		all = ();

		// default lighting event. Allows to play light Pattern style:
		// Pbind(\type, \light, \method, \dim, \data, Pwhite(0.1, 0.9, 5), \dur, 1).play
		Event.addEventType(\mblight, {
			var patcher;
			if(~patcher.isNil, {
				patcher = DmxPatcher.default;
			}, {
				patcher = DmxPatcher.all[~patcher];
			});
			//currentEnvironment.postln;
			if(patcher.notNil, {
				patcher.message(currentEnvironment);
			}, {
				"DmxPatcher not reachable!".postln;
			});
		});
	}

	*new { |id, callback|
		^super.new.init(id, callback);
	}

	init { |myid, callback|
/*		buffer = List.newClear(512).fill(0);*/
		fixtures = List();
		groups = IdentityDictionary();
		buffers = List();
		busses = List();
		server = Server.default;
		fps = 60;

		if(default==nil, { // make this the default patcher if none is there...
			default = this;
		});
		all.add(myid -> this);

		if(myid.isKindOf(Symbol).not, {
			"ID must be a symbol!".postln;
			^nil;
		});
		id = myid;

		if(callback.isNil.not, {
			server.waitForBoot({ callback.() });
		}, {
			server.waitForBoot();
		});
	}

	// makes this patcher the default patcher
	makeDefault {
		default = this;
	}

	end {
		// frees buses, stop routines, remove fixtures?
		fixtures.do({ |dev|
			dev[\routine].stop;
			this.freeBusesForFixture(dev);
		});
		buffers.size.do({
			buffers.pop.close;
		});
		all.removeAt(id);
		if((default == this) && (all.size > 0), {
			default = all[all.keys.asArray.at(0)];
		});
		if((default == this) && (all.size == 0), {
			default = nil;
		});
	}

	addBuffer { |buffer|
		// a buffer must react to the set method!
		buffers.add(buffer);
	}
	setBuffers { |dmx, addr|
/*		"trying to set data to buffers:".postln;*/
/*		[dmx, addr].postln;*/
		buffers.do({ |buf|
/*			buf.set(dmx, addr);*/
			buf.set(dmx, addr - 1); // dmx starts at 1, everything else in the world at 0
		});
	}
	removeBuffer { |index|
		buffers.at(index).close();
		buffers.removeAt(index);
	}

	addFixture { |myFixture, myGroup|
		// add fixture to internal list of fixtures
		// register OSC path/address/methods? Or pass methods to DmxFixture... better not, otherwise
		//   I have 5 methods for each fixture in memory... => call methods when fixture gets called!

		var fixtureNum;
		var fixture = (); // holds fixture to add later
		var buses; // kr-buses used for data...
		var routine; // update-routine which calls actions periodically

		buses = this.makeBussesForFixture(myFixture);
		routine = this.makeRoutineForFixture(myFixture, buses);

		fixture[\fixture] = myFixture;
		fixture[\buses] = buses;
		fixture[\routine] = routine;

		fixtures.add(fixture);
		fixtureNum = fixtures.size - 1;

		if(myGroup.notNil, {
			if(groups[myGroup].isNil, {
				groups.put(myGroup, List());
			});
			groups[myGroup].add(fixture);
		});

		// call init message as default...
		if(myFixture.hasMethod(\init), {
			myFixture.action(\init);
			this.setBuffers(myFixture.dmx, myFixture.address);
		});

		// create busses for each method, get their data in a routine or something...
	}
	makeBussesForFixture { |myFixture|
		// make bus for every method, store in busses-List
		var buses = Dictionary(); // key->value list
		// somewhereHere...
		var reservedKeys = ['channels', 'chNames', 'init', 'numArgs'];
		DmxFixture.types[myFixture.type].keysValuesDo({ |method|
			// do for each method, but omit reserved keys 'channel', 'init', 'numArgs:
			if(reservedKeys.includes(method) == false, {
				var numArgs = DmxFixture.types[myFixture.type].numArgs[method];
				buses.put(method, Bus.control(server, numArgs));
			});
		});
		^buses;
	}
	freeBusesForFixture { |myFixture|
		myFixture[\buses].do({ |bus|
			bus.free;
		});
	}
	makeRoutineForFixture { |fixture, buses|
		var routine = Routine.run({
			var val, lastval = ();
			inf.do({
				buses.keysValuesDo({ |method, bus|
					// btw: asynchronous access is way to slow as well...
					val = bus.getnSynchronous;
					if(val != lastval[method], {
						fixture.action(method, val);
						this.setBuffers(fixture.dmx, fixture.address);
					});
					lastval[method] = val;
				});
				(1/fps).wait;
			});
		});
		^routine;
	}

	removeFixture { |index|
		groups.keysValuesDo({ |grpname, fixtures|
			fixtures.do({ |dev, n|
				if(dev == fixtures[index], {
					this.removeFixtureFromGroup(n, grpname);
				});
			});
		});
		fixtures[index][\routine].stop;
		this.freeBusesForFixture(fixtures[index]);
		fixtures.removeAt(index);
	}

	nextFreeAddr { |numChans = 1|
		var chans = nil!512;
		var freeChan = nil;
		var cntr, n;
		fixtures.do({ |dev|
			var channels = DmxFixture.types.at(dev.fixture.type).at(\channels);
			var address = dev.fixture.address.asInteger;
			for(address, (address + channels - 1), { |n|
				chans[n] = 1;
			});
		});
		// 2nd approach: Step through chans, run counter that adds up if channel is free and otherwise resets, once counter = numChans -> found free slot!
		cntr = 0;
		n = 0;
		while({(cntr < numChans) && (n < 512)}, {
			if(chans[n].isNil, {
				cntr = cntr + 1;
			}, {
				cntr = 0;
			});
			n = n + 1;
		});
		if(cntr == numChans, {
			// found one!
			freeChan = n - numChans;
		});
		^freeChan;
	}


	groupNames {
		var names = [];
		groups.keysValuesDo({ |name|
			names = names.add(name)
		});
		^names;
	}
	addGroup { |groupname|
		if(groups.at(groupname.asSymbol).isNil, {
			groups.put(groupname.asSymbol, List());
		});
	}
	removeGroup { |group|
		if(group.isKindOf(Symbol), {
			groups.removeAt(group);
		});
		if(group.isKindOf(Integer), {
			"fix me!".postln;
/*			groups.removeAt(group);*/
		});
	}
	addFixtureToGroup { |fixture, group|
		groups[group].add(fixture);
	}
	removeFixtureFromGroup { |fixtureIndx, group|
		groups[group].removeAt(fixtureIndx);
	}

	numFixtures { |group = nil|
		if(group.isNil, {
			^fixtures.size;
		}, {
			^groups[group].size;
		});
	}

	busesForMethod { |method, fixtureList|
		var buses = List();
		if(fixtureList.isNil, {
			fixtureList = fixtures;
		});
		fixtureList.do({ |dev, i|
			if(dev.fixture.hasMethod(method), {
				buses.add(dev.buses[method]);
			});
		});
		^buses;
	}
	numBusesForMethod{ |method, fixtureList|
		var numbuses = 0;
		if(fixtureList.isNil, {
			fixtureList = fixtures;
		});
		fixtureList.do({ |dev, i|
			if(dev.fixture.hasMethod(method), {
				numbuses = numbuses +1;
			});
		});
		^numbuses;
	}
	busesForGroupMethod { |group, method|
		var fixtureList = groups[group];
		^this.busesForMethod(method, fixtureList);
	}
	numBusesForGroupMethod { |group, method|
		var fixtureList = groups[group];
		^this.numBusesForMethod(method, fixtureList);
	}


	message { |msg|
		// dispatches message, calls methods on fixtures, sends dmx data to buffer
		// possible message addresses:
		//   group: /{patcher}/group {method} - call method on each fixture in group
		//   group: /{patcher}/group {n} {method} - call method on {n}'th fixture in group
		//   fixture: /{patcher}/fixtures {method} - call method on every deivce in patcher (which supports this specific method)
		//   fixture: /{patcher}/fixtures {n} {method} - call method on {n}'th fixture in patcher
		//   patcher: /{method} - call method on every fixture in patcher, same as /fixture/{method}
		/*
		 * OR:
		 * event-messages:
		msg = (
			group: \ring,
			method: \color,
			data: [24, 34, 12]
		);
		e = ()
		e[\play].def.sourceCode

		DmxPatcher.message(msg); => dispatch to group/fixture, call often!

		*/
		if(msg[\group] != nil, {
/*			"make group message".postln;*/
			this.groupsMsgEvent(msg);
		}, {
			// otherwise make fixture message, which calls message on all fixtures if none is given
/*			"make fixture message".postln;*/
			this.fixturesMsgEvent(msg);
		});

	}

	fixturesMsgEvent { |msg, fixtureList = nil|
		var fixtureNums = msg[\fixture]; // can be array...
		var method = msg[\method];
		var data = msg[\data];

		// 'default' fixture list are the fixtures of the patcher
		if(fixtureList == nil, {
			fixtureList = fixtures;
		});

		if(fixtureNums == nil, {
			// apply to all fixtures in patcher
			fixtureNums = (0..(fixtureList.size-1))
		});
		if(fixtureNums.isKindOf(Array).not, {
			fixtureNums = [fixtureNums];
		});
		fixtureNums.do({ |num, i|
			if(fixtureList[num % fixtureList.size].fixture.hasMethod(method), {
				// wrap fixtures index, just to be sure...
/*				fixtureList[num % fixtureList.size].action(method, data);*/
/*				this.setBuffers(fixtureList[num%fixtureList.size].dmx, fixtureList[num%fixtureList.size].address);*/
				// rewrite: write data to bus instead of fixture directly.
				if(data.isKindOf(Array), {
					fixtureList[num % fixtureList.size].buses[method].setn(data);
				}, {
					fixtureList[num % fixtureList.size].buses[method].set(data);
				});
			});
		});
	}
	groupsMsgEvent { |msg|
		// reroute call to fixturesmsgevent, but with 'filtered' list of fixtures...
		var group = msg[\group];
		var groupFixtures = groups[group];
		this.fixturesMsgEvent(msg, groupFixtures);
	}

	// basically OSCFunc callbacks... get: msg, time, addr, and recvPort
	// a little deprecated!
	fixturesMsg { |msg, time, addr, recvPort|
		// msg[0] is address, msg[1] and following are arguments
/*		msg.postln;*/
/*		msg.do({ |d| d.class.postln });*/
		if(msg[1].isKindOf(Integer), {
			var fixtureNum = msg[1];
			var method = msg[2];
			var arguments = List();
			(msg.size-3).do({ |i|
				arguments.add(msg[i + 3]);
			});
/*			[method, arguments].postln;*/
			if(fixtureNum < fixtures.size, {
				fixtures[fixtureNum].action(method, arguments);
				this.setBuffers(fixtures[fixtureNum].dmx, fixtures[fixtureNum].address);
			}, {
				"fixture doesn't exist in patcher!".postln;
			});
		}, { // else: method called on all fixtures
			var method = msg[1];
			var arguments = List();
			(msg.size-2).do({ |i|
				arguments.add(msg[i + 2]);
			});
			fixtures.do({ |fixture, i|
				if(fixture.hasMethod(method), {
					fixture.action(method, arguments);
					this.setBuffers(fixture.dmx, fixture.address);
				});
			});
		});
	}

	groupsMsg {|msg, time, addr, recvPort|
/*		msg.postln;*/
		var group = msg[1];
		var groupDevs = groups[group];
		var method = msg[2];
		var arguments = List();
		(msg.size-3).do({ |i|
			arguments.add(msg[i + 3]);
		});
		groupDevs.do({|dev, i|
			if(dev.hasMethod(method), {
				dev.action(method, arguments);
				this.setBuffers(dev.dmx, dev.address);
			});
		});
	}


	// now a patcher gets NodeProxys registered for methods. Those NodeProxy should play out
	// .kr signals, whose values are being used to call the registered methods on the certain
	// fixture or group of fixtures.
	// Since a method might require multiple arguments, multichannel busses are needed. There
	// is no way of having actual groups of busses or something like that, so there must be
	// arguments * channels busses available (in case there are different values for different
	// fixtures). The fixtures wrap around the available fixtures in any certain group.

	// Or: a patcher registers busses for methods. Then NodeProxys need to play to those busses.
	// (A NodeProxy with Out.kr appareantly doesn't even create it's own private bus, see NP.busLoaded.)
	// deprecated!!
	makeBusForMethod { |method, numArgs = 1, group = nil, channels = 1|

		var bus = (); // bus proto...
		var numFixtures = this.numFixtures(group);
		// how do I get the number of arguments for a method??? I don't! => numArgs...

		if(server.pid == nil, {
			"Boot server first!!".postln;
			^false;
		});

		if(group.notNil, {
			bus[\group] = group;
		});

		bus.numArgs = numArgs;
		bus.channels = channels; // notice that bus.channels != bus.bus.numChannels, the latter is channels*numArgs!
		bus.method = method;

		bus.bus = Bus.control(server, numArgs * channels);

		bus.routine = Routine.run({
			var busdata;
			var message = ();
			inf.do({
				message[\method] = bus.method;
				if(bus.group.notNil, {
					message[\group] = group;
				});
				// wrap around things? hmmm...
				// if there is 1 channel, call on any fixture. if there are >1 channels, call on
				// each fixture
				if(bus.channels == 1, {
					// bus contains only data for 1 channel so it also must be numArgs big...
					message[\data] = bus.bus.getnSynchronous;
/*					message.postln;*/
					this.message(message);
				}, {
					// for each fixture get data from bus (!offset!), wrap bus channels...
					busdata = bus.bus.getnSynchronous; // .getnAt doesn't exist...
					numFixtures.do({ |i|
						var offset = i*bus.numArgs;
						message[\fixture] = i;
						// wrapAt with an array gives array of values at indizies given by array
						message[\data] = busdata.wrapAt((offset..(offset+numArgs-1)));
						this.message(message);
					});
				});
				(1/fps).wait;
			});
		});

		// add bus to bus-dictionary
		busses.add(bus);
	}
	removeBus { |index|
		if (busses[index].notNil, {
			busses[index].routine.stop;
			busses[index].bus.free;
			busses.removeAt(index);
		}, {
			("Nothing found at index "+index).postln;
		});
	}


	havefun { |group = 'stage'|
		"Fun".postln;
		if(aFun.notNil, { aFun.free });
		aFun = {
			var buses = this.busesForMethod(\color);
			var point1 = LFNoise1.kr(8.3/120).range(0, 4).fold(0, 1).lag3(0.5);
			var point2 = LFNoise2.kr(7.2/130).range(0, 4).fold(0, 1).lag3(0.5);
			buses.do({ |bus, n|
				var position = 1/buses.size * n;
				var distance = (position - [point1, point2]).abs;
				var sins = SinOsc.kr(0, distance[0] * 2pi, 0.5, 0.5)
						+ SinOsc.kr(0, distance[1] * 2pi, 0.5, 0.5)
						+ SinOsc.kr(0, distance.sum * pi)
						+ SinOsc.kr(0, distance.sum / 2 * pi + LFTri.kr(1/18.83), 0.4)
							/ 3.6;
				var color = Hsv2rgb.kr(sins.fold(0, 1), 1, 1).lag3(2);
				Out.kr(bus, color * Line.kr(0, 1, 5));
			});
			0;
		}.play;
	}

	// end vegas mode
	enoughfun {
		Routine.run({
			var black;
			black = {
				var buses = this.busesForMethod(\color);
				var color = [0, 0, 0];
				buses.do({ |bus, n|
					var sig = In.kr(bus, 3);
					ReplaceOut.kr(bus, sig * Line.kr(1, 0, 5, doneAction: 2));
				});
				0;
			}.play(addAction: \addToTail);
			4.wait;
			aFun.free;
			aFun = nil;
		}, nil, AppClock);
	}
}