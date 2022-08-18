DmxGui {
	*new { |silent = false, loadSettings|
		^super.new.init(silent, loadSettings);
	}

	init { |silent = false, loadSettings|
		DmxGui_Main(silent, loadSettings);
	}

}


DmxGui_Main {

	var <window;
	var server;
	var updateActions;
	var settingsFile;

	// silent mode doesn't open any window but allows to load default settings
	*new { |silent = false, loadSettings|
		^super.new.init(silent, loadSettings);
	}

	init { |silent = false, loadSettings|
		server = Server.default;
		updateActions = List();
		settingsFile = Platform.userConfigDir+/+"DmxGuiConfig.scd";
		this.checkForSettings;

		if(silent, {
			if(loadSettings.notNil, {
				this.loadSettings(loadSettings.asSymbol);
			}, {
				this.loadSettings(\default);
			});
		});
		"DmxGui?".postln;
		if(silent.not, {
			"DmxGui!".postln;
			window = Window.new("Dmx Setup Gui", Rect(400, 400, 400, 400)).front;
			this.makeDefaultWindow();
			this.updateView();
		});
	}

	checkForSettings {
		var emptySettings;
		if(File.exists(settingsFile).not, {
			// create default (empty) settings file...
			emptySettings = ().asCompileString;
			File.use(settingsFile, "w", { |f|
				f.write(emptySettings);
			});
		});
	}

	makeDefaultWindow {
		window.layout_(
			VLayout(
				this.serverCheckView(),
				this.patcherView(),
				this.theSaviour();
				)
		);
	}

	serverCheckView {
		var view;
		var string = "Server:";
		var txt, btn1, btn2, updateAction;
/*		var text = StaticText(view, 200@20).string_();*/

		if(server.pid.isNil, {
			string = string + "Not running!";
		}, {
			string = string + "Running...";
		});

		txt = StaticText(nil, 200@20).string_(string);

		btn1 = Button().states_([
			["Boot Server", Color.black, Color.white],
			["Stop Server", Color.black, Color.red] ])
		.action_({ |view|
			if(view.value == 1, {
				server.boot;
				this.updateView();
			}, {
				server.quit;
				this.updateView();
			});
		});
		btn2 = Button().states_([["Reboot", Color.black, Color.white]])
			.action_({
				DmxPatcher.all.do({|patcher|
					patcher.end();
				});
				thisProcess.recompile;
			});
		updateAction = {
			var string;
			if(server.pid.isNil, {
				string = "Server: Not running!";
				btn1.value_(0);
			}, {
				string = "Server: Running...";
				btn1.value_(1);
			});
			txt.string_(string);
		};
		updateAction.value();
		updateActions.add(updateAction);

		view = HLayout(txt, btn1, btn2);

		^view;
	}

	patcherView {
		var view;
		var hdln;
		var ptchrbx, btns, addr, rmvr;

		// headline...
		hdln = StaticText(nil, 200@20).string_("Patcher:")
			.font_(Font.sansSerif(18, true));

		// box that lists all the active patchers
		ptchrbx = ListView(nil, Rect(0, 0, 200, 200));
		updateActions.add({
			var ptchrs = DmxPatcher.all;
			var ptchrarr = [];
			if(ptchrs.notNil, {
				ptchrs.keysDo({ |ptchr|
					ptchrarr = ptchrarr.add(ptchr);
				});
				ptchrbx.items_(ptchrarr);
			});
		});

		// buttons for edit/manage fixtures/remove of patcher
		rmvr = Button().states_([ ["Remove Patcher"] ])
			.action_({
				var patcherName = ptchrbx.items.at(ptchrbx.value);
				DmxPatcher.all.at(patcherName).end();
/*				ptchrbx.items = ptchrbx.items.removeAt(ptchrbx.value);*/
				this.updateView();
			});
		btns = VLayout(
			HLayout(
				[Button().states_([ ["Setup"] ])
					.action_({DmxGui_SetupPatcher(ptchrbx.items.at(ptchrbx.value))}),
				 a:\top],
				[Button().states_([ ["Fixtures"] ])
					.action_({DmxGui_manageFixtures(ptchrbx.items.at(ptchrbx.value))}),
				a:\top]
			),
			rmvr;
		);

		// button: add patcher...
		addr = Button().states_([ ["Add Patcher"] ])
			.action_({
				this.addPatcher;
			});

		view = VLayout(
			hdln,
			HLayout(ptchrbx, btns),
			addr
		);

		^view;
	}

	addPatcher {
		var dialog;
		var bounds = Window.availableBounds;
		var addbtn, ptchrname;

		dialog = Window("Add Patcher", Rect(bounds.width/2-200, bounds.height/2+50, 400, 100));

		ptchrname = TextField();

		addbtn = Button().states_([["Create Patcher"]])
			.action_({ |btn|
				var patcher = DmxPatcher.all();
				var check = true;
				if(ptchrname.value == "", {
					ptchrname.string = "default";
/*					check = false;*/
				});
				if(patcher[ptchrname.value.asSymbol].notNil, {
					"Patcher already exists!".postln;
					check = false;
				});
				if(check, {
					dialog.close;
					DmxPatcher.new(ptchrname.value.asSymbol);
					this.updateView();
				});
			});

		dialog.layout_(VLayout(
			HLayout(
				StaticText().string_("Patcher name:"),
				ptchrname
			),
			[addbtn, a:\topleft]
		));

		dialog.front;
	}

	theSaviour {
		var view;
		var slctr, ldbtn, svbtn;
		var settingsObj, makeitems;

		slctr = PopUpMenu();

		makeitems = {
			var items = [];
			File.use(settingsFile, "r", { |f|
				settingsObj = f.readAllString.interpret;
			});
			settingsObj.keysValuesDo({ |name, obj|
				items = items.add(name);
			});
			// load "default" settings on boot, if they exist...
			if(settingsObj[\default].notNil, {
				this.loadSettings(\default);
			});

/*			Archive.read("DmxGuiArchive.scd");
			Archive.global.dictionary.keysValuesDo({ |name, obj|
				items = items.add(name);
			});*/
			slctr.items_(["New..."]++items);
		};
		makeitems.value();

/*		slctr.items_(["New..."]++makeitems.value());*/

		ldbtn = Button().states_([["Load"], ["Loaded!", Color.black, Color.green]])
			.action_({
				if(slctr.value > 0, {
					this.loadSettings(slctr.items[slctr.value]);
					Routine.run({
						2.do({
							defer{ldbtn.value = 1};	0.1.wait;
							defer{ldbtn.value = 0};	0.1.wait;
						});
					});
				})
			});
		svbtn = Button().states_([["Save"], ["Saving..."], ["Saved!", Color.black, Color.green]])
			.action_({
				var name;
				if(slctr.items.at(slctr.value) == "New...", {
					DmxGuiDialog("Name new preset", { |dialog|
						var txtFld, btn;
						txtFld = TextField();
						btn = Button().states_([["Save new preset"]])
							.action_({
								this.saveSettings(txtFld.value);
								dialog.close;
								makeitems.value();
							});
						dialog.layout_(
							VLayout(txtFld, btn);
						);
					});
				}, {
					name = slctr.items[slctr.value];
					this.saveSettings(name);
					makeitems.value();
					Routine.run({
						2.do({
							defer{svbtn.value = 2};	0.1.wait;
							defer{svbtn.value = 0};	0.1.wait;
						});
					});
				});
			});

		view = VLayout(
			StaticText().string_("Load/Save Setup").font_(Font.sansSerif(18, true)),
			HLayout(
				slctr, ldbtn, svbtn
			)
		);

		^view;
	}
	loadSettings { |name|
		var settings, file;
		file = File.open(settingsFile, "r");
		settings = file.readAllString.interpret;
		file.close;
/*		Archive.read("DmxGuiArchive.scd");*/
		if(settings[name.asSymbol].notNil, {
			DmxPatcher.all.do({ |ptchr|
				ptchr.end;
			});
			server.waitForBoot({
				server.sync;
				this.applySettings(settings[name.asSymbol]);
/*				this.applySettings(Archive.global[name.asSymbol]);*/
				this.updateView;
			});
		}, {
			("Settings " ++ name.asString ++ " not found!!").postln;
		});
	}
	applySettings { |settings|
		settings.patchers.do({ |ptchr|
			var patcher;
			patcher = DmxPatcher.new(ptchr.name);
			ptchr.buffers.do({ |buf|
				var instance = buf.classname.new;
				buf.devices.do({ |adevCompileString|
					var dv = adevCompileString.interpret;
					instance.addDevice(dv);
				});
			});
			ptchr.fixtures.do({ |dev|
				patcher.addFixture(DmxFixture.new(dev.type.asSymbol, dev.address));
			});
			ptchr.groups.do({ |grp|
				patcher.addGroup(grp.name);
				grp.deviceIndizes.do({ |indx|
					patcher.addFixtureToGroup(patcher.fixtures.at(indx), grp.name);
				});
			});
		});
	}
	saveSettings { |name|
		var file, savedData;
		file = File.open(settingsFile, "r");
		savedData = file.readAllString.interpret;
		file.close;
		savedData[name.asSymbol] = this.saveObject;
		savedData = savedData.asCompileString;
		file = File.open(settingsFile, "w"); // open again to empty contents...
		file.write(savedData);
		file.close;
/*		var data;
		Archive.read("DmxGuiArchive.scd");
		Archive.global[name.asSymbol] = this.saveObject;
		Archive.write("DmxGuiArchive.scd");*/
	}
/*	Platform.userConfigDir*/
/*	Platform.resourceDir*/
	saveObject {
		// save:
		// * patcher - name, connected Buffers, connected Output Fixtures (with arguments?)
		// * fixtures - address, group
/*		DmxPatcher.all.at(\default).asCompileString*/
		// as an event with .asCompileString:
/*		(patcher: (name: \default, buffers: (['bufer1', 'buffer2']))).asCompileString*/

		var data = ();
		var ptchrs = DmxPatcher.all;
		data.patchers = List();
		ptchrs.keysValuesDo({ |patcherid, patcher|
			var myPatcher = ();
			var myTempFixtures; //
			// get patcher info:
			myPatcher.name = patcherid;

			myPatcher.buffers = List();
			patcher.buffers.do({ |buf, n|
				var buffer = (classname: buf.class);
				var devices = buf.devices;
				buffer.devices = List();
				devices.do({ |dev, m|
					var device = ();
					buffer.devices.add(dev.compileString);
				});
				myPatcher.buffers.add(buffer);
			});

			myPatcher.fixtures = List();
			myTempFixtures = Dictionary();
			patcher.fixtures.do({ |fixture, n|
				var myFix = (type: fixture.type, address: fixture.address);
				myPatcher.fixtures.add(myFix);
				myTempFixtures.add(n -> fixture);
			});

			myPatcher.groups = List();
			patcher.groups.keysValuesDo({ |grpname, devs|
				var myGrp = (name: grpname, deviceIndizes: List());
				devs.do({ |dev|
					var devindx = myTempFixtures.findKeyForValue(dev);
					if(devindx.notNil, {
						myGrp.deviceIndizes.add(devindx);
					});
				});
				myPatcher.groups.add(myGrp);
			});

			data.patchers.add(myPatcher);
		});
		^data;
	}
	saveString {
		^this.saveObject.asCompileString;
	}

	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}

/*	Window.availableBounds*/

}


DmxGui_SetupPatcher {

	var window;
	var updateActions;
	var patcher;

	*new { |patcherid|
		^super.new.init(patcherid);
	}

	init { |patcherid|
		updateActions = List();
		patcher = DmxPatcher.all.at(patcherid);
		this.setupPatcher();
		this.updateView();
	}

	setupPatcher { |patcherid|
		var bfrslst, devslst;
		var addBfrBtn, rmvBfrBtn;
		var devslctr, addDevBtn, rmvDevBtn;

		window = Window("Setup Patcher" + patcher.id);

		bfrslst = ListView()
			.action_({ this.updateView() });
		updateActions.add({
			var val = bfrslst.value;
			bfrslst.items = patcher.buffers.collect({ |bfr, n|
				"A Buffer";
			});
			if(val.notNil, {
				if(val < bfrslst.items.size, {
					bfrslst.value = val;
				});
			});
		});

		// addBfrBtn = Button().states_([["Add Buffer"]])
		// .action_({
		// 	this.addBufferToPatcher(patcher);
		// 	this.updateView();
		// });
		// rmvBfrBtn = Button().states_([["Remove Buffer"]])
		// .action_({
		// 	var buffer = bfrslst.items.at(bfrslst.value());
		// 	if(buffer.notNil, {
		// 		this.removeBufferFromPatcher(bfrslst.value, patcher);
		// 		this.updateView();
		// 	});
		// });

		devslst = ListView();
		updateActions.add({
			if(bfrslst.value().notNil, {
				var buffer = patcher.buffers.at(bfrslst.value());
				var items = [];
				buffer.devices().array.do({ |dev|
					var str = dev.asString;
					if({dev.describe}.try.notNil, {
						str = str + "("++dev.describe++")";
					});
					items = items.add(str);
				});
				devslst.items = items;
			}, {
				devslst.items = [];
			});
		});

		devslctr = PopUpMenu()
			.items_(DmxBuffer.knownDevices.collect({|dev| dev.asString}));
		addDevBtn = Button().states_([["Add Device to Buffer"]])
			.action_({
				var devclass = devslctr.items.at(devslctr.value());
				this.addDeviceToBuffer(devclass, patcher.buffers.at(bfrslst.value()))
			});
		rmvDevBtn = Button().states_([["Remove Device from Buffer"]])
			.action_({
				var buffer = patcher.buffers.at(bfrslst.value());
				var devIndex = devslst.value();
				this.removeDeviceFromBuffer(devIndex, buffer);
			});

		window.layout = VLayout(
			StaticText().string_("Buffers:").font_(Font.sansSerif(18, true)),
			bfrslst,
			HLayout(addBfrBtn, rmvBfrBtn),
			StaticText().string_("Output Devices:").font_(Font.sansSerif(18, true)),
			devslst,
			[devslctr, a:\left],
			HLayout(addDevBtn, rmvDevBtn)
		);

		window.front;
	}
	// addBufferToPatcher { |patcher|
	// 	var buffer = DmxBuffer();
	// 	patcher.addBuffer(buffer);
	// }
	// removeBufferFromPatcher { |index, patcher|
	// 	/*		var bufkey = patcher.buffers.find([buffer]);*/
	// 	patcher.removeBuffer(index);
	// }

	addDeviceToBuffer{ |devclass, buffer|
		var theclass = devclass.asSymbol.asClass;
		if(theclass.class.methods[0].argNames.size > 1, {
			this.devOptionsWindow(theclass, buffer);
		}, {
			var device = theclass.new();
			buffer.addDevice(device);
			this.updateView();
		});
	}
	devOptionsWindow { |devclass, buffer|
		var lilwin, argpairs, argpairlayout, addbtn;
		var bounds = Window.availableBounds;

		lilwin = Window("Arguments for" + devclass.asString, Rect(bounds.width/2-150, bounds.height/2+100, 300, 200));
		lilwin.layout = VLayout();
		lilwin.layout.add(
			StaticText().string_("Arguments needed for"+devclass.asString)
			.font_(Font.sansSerif(18, true)), align:\topLeft);

		argpairs = [];
		devclass.class.methods[0].argNames.do({ |anarg|
			if(anarg != \this, {
				argpairs = argpairs.add([anarg.asSymbol, TextField()]);
			});
		});
		argpairs.do({ |anarg|
			lilwin.layout.add(HLayout(
				[StaticText().string_(anarg[0].asString), a:\top],
				anarg[1]
			), align:\top);
		});

		addbtn = Button().states_([["Add Device to Buffer"]])
			.action_({
				var newclass = devclass.asString ++ ".new(";
				var myargs = [];
				var device;
				argpairs.do({ |pair|
					if(pair[1].notNil, {
						myargs = myargs.add(pair[1].value);
					});
				});
				newclass = newclass ++ myargs.join(',') ++ ")";
				device = newclass.interpret;
				buffer.addDevice(device);
				lilwin.close;
				this.updateView();
			});

		lilwin.layout.add(addbtn);
		lilwin.front;
	}
	removeDeviceFromBuffer { |devIndex, buffer|
		buffer.removeDevice(devIndex);
		this.updateView();
	}

	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}

}

DmxGui_manageFixtures {

	var window;
	var updateActions;
	var patcher;

	*new { |patcherid|
		^super.new.init(patcherid);
	}

	init { |patcherid|
		updateActions = List();
		patcher = DmxPatcher.all.at(patcherid);
		this.manageFixtures();
		this.updateView();
	}

	manageFixtures {
		var window;
		var bounds = Window.availableBounds;

		var grpslist, grpAdd, grpRmv;

		var devslist;
		var devslctr;
		var addrTxt, autoAddr, addBtn, rmvDevGrp, rmvBtn;

		window = Window("Manage Fixtures", Rect(bounds.width/2-200, bounds.height/2+50, 400, 400));

		grpslist = ListView().selectionMode_(\extended)
			.action_({ |list|
				this.updateView;
			});
		updateActions.add({
			var val = grpslist.value ?? 0;
			grpslist.items_(["(none)"] ++ patcher.groupNames).value_(val);
		});
		grpAdd = Button().states_([["Add Group"]])
			.action_({ this.addGroup });
		grpRmv = Button().states_([["Remove Group"]])
			.action_({
				if(grpslist.value.notNil && (grpslist.value > 0), {
					patcher.removeGroup(patcher.groupNames.at(grpslist.value - 1));
				});
				this.updateView();
			});

		devslist = ListView();
		updateActions.add({
			var items = [];
			var fixtures;
			if(grpslist.value > 0, {
				fixtures = patcher.groups[grpslist.items[grpslist.value]];
			}, {
				// all fixtures if 'none' is selected
				fixtures = patcher.fixtures;
			});
			fixtures.do({ |fixture|
				items = items.add(fixture.type.asString + "- addr:" + fixture.address);
			});
			devslist.items_(items);
		});

		devslctr = PopUpMenu()
			.items_(DmxFixture.typeNames().collect({ |name| name.asString+"("++DmxFixture.types.at(name).channels++"ch)" }))
			.action_({this.updateView});

		addrTxt = TextField();
		updateActions.add({
			if(autoAddr.value, {
				var devType = DmxFixture.types[DmxFixture.typeNames.at(devslctr.value)];
				addrTxt.string_(patcher.nextFreeAddr(devType.channels));
			});
		});
		autoAddr = CheckBox().value_(true);
		addBtn = Button().states_([["Add Fixture"]])
			.action_({
				var devTypeName = DmxFixture.typeNames.at(devslctr.value);
				var grp = nil;
				if(grpslist.value > 0, { grp = grpslist.items[grpslist.value]; });
				patcher.addFixture(DmxFixture(devTypeName, addrTxt.value.asInteger), grp);
				this.updateView();
			});
		rmvDevGrp = Button().states_([["Remove Fixture from Group"]])
			.action_({
				if(grpslist.value > 0, {
					var group = grpslist.items[grpslist.value];
					var devindex = devslist.value();
					patcher.removeFixtureFromGroup(devindex, group);
				});
				this.updateView;
			});
		rmvBtn = Button().states_([["Remove Fixture"]])
			.action_({
				var fixtures, fixture, group, fixIndx;
				if(grpslist.value > 0, {
					fixtures = patcher.groups[grpslist.items[grpslist.value]];
					group = grpslist.items[grpslist.value];
				}, {
					fixtures = patcher.fixtures;
				});
				fixture = fixtures[devslist.value];
				patcher.fixtures.do({ |fix, n|
					if(fix == fixture, { fixIndx = n });
				});
/*				fixIndx = patcher.fixtures.find([fixture]);*/
/*				if(group.notNil, { patcher.removeFixtureFromGroup(devslist.value, group) });*/
				patcher.removeFixture(fixIndx);
				this.updateView();
			});

		window.layout = VLayout(
			StaticText().string_("Groups:").font_(Font.sansSerif(18, true)),
			grpslist,
			HLayout(grpAdd, grpRmv),
			StaticText().string_("Fixtures:").font_(Font.sansSerif(18, true)),
			devslist,
			StaticText().string_("Add Fixture:").font_(Font.sansSerif(18, true)),
			HLayout(StaticText().string_("Fixture-type:"), devslctr),
			HLayout([StaticText().string_("Address:"), s:1], [addrTxt, s:1]),
			HLayout(StaticText().string_("Auto-Address:"), [autoAddr, a:\right]),
			addBtn,
			HLayout(rmvDevGrp, rmvBtn)
		);
		window.front;
	}

	addGroup {
		var dialog;
		var bounds = Window.availableBounds;
		var addbtn, grpname;

		dialog = Window("Add Patcher", Rect(bounds.width/2-200, bounds.height/2+50, 400, 100));

		DmxGuiDialog("Add Group", {|dialog|
			var grpname, addbtn;
			grpname = TextField();
			addbtn = Button().states_([["Create Group"]])
				.action_({ |btn|
					var check = true;
					patcher.groupNames.do({ |name|
						if(name == grpname.string, {
							check = false;
							"Group exists already!".postln;
						});
					});
					if(grpname.string == "", {
						check = false;
						"No Groupname given!".postln;
					});
					if(check, {
						dialog.close;
						patcher.addGroup(grpname.string);
						this.updateView();
					});
				});
			dialog.layout_(VLayout(
				HLayout(
					StaticText().string_("Group name:"),
					grpname
				),
				[addbtn, a:\topleft]
			));
		});
	}

	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}
}

DmxGuiDialog {
	var window;

	*new { |title, fn|
		^super.new.init(title, fn);
	}

	init { |title, fn|
		var bounds = Window.availableBounds;
		window = Window(title, Rect(bounds.width/2-100, bounds.height/2+50, 200, 100));
		fn.value(window);
		window.front;
	}

}
