<!-- fork-homepage-detail:v1:start -->
# Eclipse PDE fork: build and understand Eclipse plug-ins

PDE, the Plug-in Development Environment, is Eclipse tooling for developing,
launching, testing and packaging plug-ins, OSGi bundles and rich-client
applications. This repository contains PDE's tooling itself, including manifest
and target-platform support, API Tools, editors, launch integration and tests.

[Source map](#pde-source-map) · [Concepts](#pde-key-concepts) ·
[Quickstart](#pde-quickstart) · [Example](#pde-worked-example) ·
[Fork guide](docs/FORK_GUIDE.md)

## PDE source map

| Area | Responsibility |
| --- | --- |
| [ui/](ui/) | PDE core/editor/launching tooling and UI test owners |
| [apitools/](apitools/) | API analysis, compatibility and baseline tooling |
| [ds/](ds/) | Declarative Services tooling |
| [e4tools/](e4tools/) | Eclipse 4 tooling |
| [build/](build/) | PDE Build integration; distinguish it from Tycho's Maven build |
| [features/](features/) and [repository/](repository/) | Installable feature grouping and p2 packaging |
| [docs/](docs/) and [user help](org.eclipse.pde.doc.user/) | Concepts, end-user tasks and reference documentation |

The [root POM](pom.xml) defines the actual modules and conditional repository
assembly. An IDE product, source project and Maven module are different things.

## PDE key concepts

- **Plug-in / OSGi bundle:** a modular deployment unit whose manifest declares
  identity, dependencies and exported packages.
- **Target definition:** describes the dependencies available to plug-in projects;
  it is separate from what is installed in the development IDE.
- **Feature / p2:** an installable grouping of bundles and the provisioning system
  that publishes/resolves them.
- **API baseline:** a comparison reference used to detect incompatible API changes
  and versioning requirements.
- **Execution environment:** a bundle's declared Java environment; do not confuse
  it with the JDK that runs Maven or the IDE.
- **Tycho:** Maven tooling for building Eclipse bundles, features and test harnesses.

## PDE fork lineage and status

The lineage is [Eclipse PDE](https://github.com/eclipse-pde/eclipse.pde) →
[swt-initiative31/eclipse.pde](https://github.com/swt-initiative31/eclipse.pde) →
[this hsoliwal fork](https://github.com/hsoliwal/initiative31-eclipse.pde).
The [fork guide](docs/FORK_GUIDE.md) records that the intermediate fork describes
an API Tools bug. That description alone does not establish which fix is present
or whether it is accepted upstream. The code here is PDE development tooling;
the repository name is not evidence that it contains the Initiative 31 SWT
demonstrator or a qualified rendering backend.

## PDE quickstart

At the inspected revision, [Jenkins](Jenkinsfile) selects **JDK 25**,
[the extension](.mvn/extensions.xml) selects **Tycho 5.0.1**, and
[the POM](pom.xml) uses **eclipse-platform-parent 4.41.0-SNAPSHOT**.
Choose Maven compatible with that build. Historical version examples in older
prose are not the current toolchain authority. Parent and target-platform
resolution need reachable repositories, and UI tests need a native display.

```sh
git clone https://github.com/hsoliwal/initiative31-eclipse.pde.git
cd initiative31-eclipse.pde
git rev-parse HEAD
java -version
mvn -version
mvn clean verify -Ptck
```

The Oomph button below provisions upstream PDE. Confirm which remote/branch the
workspace imported. The commands are configuration-backed examples, not a
successful build or compatibility certification from this documentation change.

## PDE command cheat sheet

```sh
# Narrow UI-test investigation using Tycho's verify lifecycle.
mvn verify -pl :org.eclipse.pde.ui.tests -Pbuild-individual-bundles
# Include PDE core when testing a local core change.
mvn verify -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests -Pbuild-individual-bundles
# Locate target resolution implementation and usages.
git grep -n 'class TargetDefinition' -- ui
```

Inspect `target/surefire-reports` for tests, `target/apianalysis` for API analysis
and `target/compilelogs` for compiler diagnostics. A missing sibling parent
directory does not by itself prove failure: the individual-bundle profile also
supports remote parent resolution. Record the actual unresolved coordinate.

## PDE worked example

**Diagnose an unresolved target before changing plug-in source.**

1. Open [Target Definitions](docs/Target_Definitions.md) and identify the target
   selected by the workspace/build, including its repository locations.
2. Distinguish the declared locations from successfully resolved bundles. A saved
   target definition can still have unresolved dependencies.
3. Follow [TargetDefinition.java](ui/org.eclipse.pde.core/src/org/eclipse/pde/internal/core/target/TargetDefinition.java):
   `resolve(...)` delegates to locations, while status handling records failures.
4. Capture the failing repository/bundle and execution environment. Fix resolution
   before interpreting downstream missing-package errors as product-source bugs.
5. Re-run the relevant UI/test lane and inspect its report. A focused UI test run
   is not full API Tools, TCK or repository coverage.

For more detail read [the user guide](docs/User_Guide.md),
[API Tools](docs/API_Tools.md), [execution environments](docs/Execution_Environments.md)
and [the fork contributor guide](docs/FORK_GUIDE.md).
Upstream project text, contributor requirements and licensing follow unchanged.

> Source tour checked at [`0bfea1ae5baa`](https://github.com/hsoliwal/initiative31-eclipse.pde/tree/0bfea1ae5baa497bd814e63ccf43897bbe4cfaa1). Commands describe that revision; recheck build configuration when changing revisions. Build/UI/performance verification was not run for this README update.

---

<!-- fork-homepage-detail:v1:end -->

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

