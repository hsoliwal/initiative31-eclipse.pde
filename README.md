> **About this fork:** This is `hsoliwal/initiative31-eclipse.pde`, forked through [swt-initiative31/eclipse.pde](https://github.com/swt-initiative31/eclipse.pde) from [Eclipse PDE](https://github.com/eclipse-pde/eclipse.pde). PDE is plug-in development tooling, not the Initiative 31 SWT demonstrator.
>
> Start with the [fork contributor guide](docs/FORK_GUIDE.md) for repository navigation, source-verified build/test entry points, toolchain caveats, and contribution destinations. The upstream project introduction follows.

Eclipse PDE - Plug-in Development Environment
=====================================================

Thanks for your interest in this project.

Project description:
--------------------

The PDE project provides the Eclipse tooling to develop plug-ins and OSGI bundles.  
Website: https://projects.eclipse.org/projects/eclipse.pde

How to contribute:
--------------------
Contributions to PDE are most welcome. There are many ways to contribute, 
from entering high quality bug reports, to contributing code or documentation changes. 
For the upstream contribution requirements, see [CONTRIBUTING.md](CONTRIBUTING.md). For this fork's source map and setup notes, see the [fork contributor guide](docs/FORK_GUIDE.md).

[![Create Eclipse Development Environment for PDE](https://download.eclipse.org/oomph/www/setups/svg/PDE.svg)](
https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/releng/org.eclipse.pde.setup/PDEConfiguration.setup&show=true
"Click to open Eclipse-Installer Auto Launch or drag into your running installer")

Developer resources:
--------------------

Information regarding source code management, builds, coding standards, and more.

- https://projects.eclipse.org/projects/eclipse.pde/developer

Contributor License Agreement:
------------------------------

Before your contribution can be accepted by the project, you need to create and electronically sign the Eclipse Foundation Contributor License Agreement (CLA).

- http://www.eclipse.org/legal/CLA.php


How to build on the command line
--------------------------------

Use a Maven/JDK toolchain compatible with the checked-out Tycho extension and Eclipse Platform parent. The [build configuration guide](docs/FORK_GUIDE.md#read-the-build-configuration-before-choosing-a-toolchain) records this snapshot's configuration and explains parent resolution and UI-test prerequisites. After setup, run from the repository root:

`mvn clean verify`

Contact:
--------

Contact the project developers via Github discussions

- https://github.com/eclipse-pde/eclipse.pde/discussions

License
-------

[Eclipse Public License (EPL) v2.0](https://www.eclipse.org/legal/epl-2.0/)

Support us with processing power
--------------------------------

If your Organizations is an [Eclipse Member](https://www.eclipse.org/membership/exploreMembership.php) you can help us by [sponsoring one of the included resource packs](https://github.com/eclipse-cbi/cbi/wiki#assigning-additional-resources-to-a-project) to speed up builds. Organizations can check how many Resource Packs they have left for project sponsoring on the [membership portal](https://membership.eclipse.org/portal/login).

The following Organizations support PDE getting processing power for their builds:
* <img src="https://www.eclipse.org/favicon.ico" width="16" height="16"> [Eclipse Foundation](https://www.eclipse.org/sponsor/) - host our CI Infrastructure, become a friend of Eclipse and support the Eclipse Foundation in general
* <img src="https://www.sap.com/favicon.ico"     width="16" height="16"> [SAP SE](https://www.eclipse.org/membership/showMember.php?member_id=665) - sponsors two resource packs with 2 CPU / 8 GB RAM each

