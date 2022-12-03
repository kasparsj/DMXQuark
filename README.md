# DMXQuark

SuperCollider DMX quark is a DMX controller framework with support for Scenes and Chases. 

It supports Serial ([Enttec DMX USB Pro](https://www.enttec.com/product/lighting-communication-protocols/dmx512/dmx-usb-pro/)) and [OLA](https://github.com/OpenLightingProject/ola) (OSC) output.

## Installation

`Quarks.install("https://github.com/kasparsj/DMXQuark.git")`

## Usage

See:

- 4 ADJ moving heads & 3 pars example: [Examples/adj.scd](Examples/adj.scd)
- 2 Cameo ThunderWashes & 4 pars example: [Examples/cameo+adj.scd](Examples/cameo+adj.scd)

## Art-Net support and simulator

SupporCollider currently does not directly support TCP/UDP networking, therefore if you need to output to Art-Net or other TCP/UDP protocol, you can use [OSC2DMX](https://github.com/kasparsj/OSC2DMX) bridge application.

[OSC2DMX](https://github.com/kasparsj/OSC2DMX) is an OSC to DMX bridge application with support for Enttec DMX USB Pro, Art-Net and simulator software.

## Todo
- add more examples
- create help files

## Contributors

This quark was based on code from:

[mblight](https://github.com/bennigraf/mblght) by Benjamin Graf
