/**
 * DmxPatcher: Registers lighting equipment of certain types (?), manages routing to actual dmx addresses
 */
DmxPatcher {
	var <fixtures;
	var <groups;
	var <id;
	// todo: refactor
	var <buffers;
	var <>fps; // fps to get data from busses with
	classvar <default; // default (usually first) DmxPatcher...
	classvar <all; // holds all opened patchers for reference...

	var aFun; // some vegas mode...

	*initClass {
		Class.initClassTree(Event);

		all = ();

		Event.addEventType(\dmx, { |server|
			var groupName, group, fixtures, instrument;
			var reservedKeys = [\patcher, \group, \fixtures, \fixture, \server, \type, \dur, \sustain, \delta];
			var patcherId = ~patcher ? \default;
			var patcher = if (patcherId.isSymbol, {
				if (patcherId == \default, {
					DmxPatcher.default;
				}, {
					DmxPatcher.all[patcherId];
				});
			}, { patcherId });
			groupName = ~group;
			group = if (groupName.isSymbol, {
				if (patcher.notNil, {
					patcher.groups[groupName];
				}, {
					"patcher % not found".format(patcherId).postln;
					nil;
				});
			}, { groupName });
			fixtures = ~fixtures ? ~fixture;
			if (fixtures.notNil or: { group.notNil }, {
				if (fixtures.notNil, {
					if (fixtures.isSequenceableCollection.not, { fixtures = [fixtures]; });
					fixtures.do { |fixture|
						fixture = if (fixture.isInteger, { group.fixtures[fixture] }, { fixture });
						currentEnvironment.keysValuesDo { |key, value|
							if (reservedKeys.indexOf(key).isNil, {
								fixture.set(key, value);
							});
						};
					};
				}, {
					currentEnvironment.keysValuesDo { |key, value|
						if (reservedKeys.indexOf(key).isNil, {
							group.set(key, value);
						});
					};
				});
			}, {
				"group % not found in patcher %".format(group, patcher.id).postln;
			});
		});
	}

	*new { |myid|
		^super.new.init(myid);
	}

	init { |myid|
		fixtures = List();
		groups = IdentityDictionary();
		buffers = List();
		fps = 60;

		if (default==nil, {
			default = this;
		});
		all.add(myid -> this);

		if(myid.isKindOf(Symbol).not, {
			"ID must be a symbol!".postln;
			^nil;
		});
		id = myid;
	}

	makeDefault {
		default = this;
	}

	end {
		// frees buses, stop routines, remove fixtures?
		fixtures.do({ |fixture|
			fixture.stop;
		});
		all.removeAt(id);
		if((default == this) && (all.size > 0), {
			default = all[all.keys.asArray.at(0)];
		});
		if((default == this) && (all.size == 0), {
			default = nil;
		});
	}

	addFixture { |myFixture, myGroup|
		var fixtureNum;

		myFixture.makeBuses();
		myFixture.makeRoutine(fps);

		fixtures.add(myFixture);
		fixtureNum = fixtures.size - 1;

		// todo: do we need default/all group?
		// if (myGroup.isNil, {
		// 	myGroup = List();
		// 	}, {
		// 		if (myGroup.isKindOf(SequenceableCollection).not, {
		// 			myGroup = List.newUsing([myGroup]);
		// 		});
		// });
		// if (myGroup.indexOf(\all).isNil, {
		// 	myGroup.add(\all)
		// });
		if (myGroup.notNil, {
			if (myGroup.isKindOf(SequenceableCollection).not, {
				myGroup = [myGroup];
			});
			myGroup.do { |group|
				if(groups[group].isNil, {
					groups.put(group, DmxGroup(group));
				});
				groups[group].add(myFixture);
			};
		});

		if (myFixture.buffer.notNil and: { buffers.indexOf(myFixture.buffer).isNil }, {
			buffers.add(myFixture.buffer);
		});

		myFixture.tryAction(\init);
	}

	removeFixture { |index|
		var fixture = fixtures[index];
		groups.keysValuesDo { |grpname, grp|
			grp.remove(fixture);
		};
		fixture.stop;
		fixtures.removeAt(index);

		if (fixture.buffer.notNil, {
			var sameBuffer = fixtures.collect { |fix, n|
				fix.buffer == fixture.buffer;
			};
			if (sameBuffer.size == 0, {
				buffers.remove(fixture.buffer);
			});
		});
	}

	nextFreeAddr { |numChans = 1|
		var chans = nil!512;
		var freeChan = nil;
		var cntr, n;
		fixtures.do({ |fixture|
			var numChannels = DmxFixture.types.at(fixture.type).at(\numChannels);
			var address = fixture.address.asInteger;
			for(address, (address + numChannels - 1), { |n|
				chans[(n-1)] = 1;
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
			freeChan = n - numChans + 1;
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
			groups.put(groupname.asSymbol, DmxGroup(groupname));
		});
	}
	removeGroup { |group|
		if(group.isKindOf(Symbol), {
			groups.removeAt(group);
		});
	}
	addFixtureToGroup { |fixture, group|
		groups[group].add(fixture);
	}

	numFixtures { |group = nil|
		if(group.isNil, {
			^fixtures.size;
		}, {
			^groups[group].size;
		});
	}

	busesForMethod { |method|
		DmxFixture.busesForMethod(method, fixtures);
	}

	numBusesForMethod{ |method|
		DmxFixture.numBusesForMethod(method, fixtures);
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
			if(fixtureList[num % fixtureList.size].hasMethod(method), {
				// wrap fixtures index, just to be sure...
/*				fixtureList[num % fixtureList.size].action(method, data);*/
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
		if(msg[1].isKindOf(Integer), {
			var fixtureNum = msg[1];
			var method = msg[2];
			var arguments = List();
			(msg.size-3).do({ |i|
				arguments.add(msg[i + 3]);
			});
			if(fixtureNum < fixtures.size, {
				fixtures[fixtureNum].action(method, arguments);
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
				});
			});
		});
	}

	groupsMsg {|msg, time, addr, recvPort|
		var group = msg[1];
		var groupFixtures = groups[group];
		var method = msg[2];
		var arguments = List();
		(msg.size-3).do({ |i|
			arguments.add(msg[i + 3]);
		});
		groupFixtures.do({|fix, i|
			if(fix.hasMethod(method), {
				fix.action(method, arguments);
			});
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
