DmxGroup {
	var <name;
	var <fixtures;

	*new { |myName|
		^super.new.init(myName);
	}

	init { |myName|
		name = myName;
		fixtures = List();
	}

	add { |fixture|
		fixtures.add(fixture);
	}

	remove { |fixture|
		fixtures.remove(fixture);
	}

	set { |arg1, arg2|
		fixtures.do({ |fixture, i|
			fixture.set(arg1, arg2);
		});
	}

	busesForMethod { |method|
		^DmxFixture.busesForMethod(method, fixtures);
	}
	numBusesForMethod { |method|
		^DmxFixture.numBusesForMethod(method, fixtures);
	}

	size {
		^fixtures.size;
	}
}