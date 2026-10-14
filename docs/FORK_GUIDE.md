# Exploring the Initiative 31 PDE fork

PDE is the tooling behind Eclipse plug-in development: manifests and dependencies,
launch configurations, API compatibility checks, features, products and update
sites. This guide gives you a source map and a concrete regression-test route.

[Command cheat sheet](COMMAND_CHEATSHEET.md) · [Upstream README retained here](../README.md) ·
[API Tools](API_Tools.md) · [Target definitions](Target_Definitions.md) · [User guide](User_Guide.md)

## Understanding the repository and its lineage

This public repository is `hsoliwal/initiative31-eclipse.pde`. Its immediate parent
is [`swt-initiative31/eclipse.pde`](https://github.com/swt-initiative31/eclipse.pde);
the original project is [`eclipse-pde/eclipse.pde`](https://github.com/eclipse-pde/eclipse.pde).

At the source snapshot reviewed on **2026-10-14**, this fork's `master` and the
immediate parent's `master` both pointed to
[`4454bcd7eb06`](https://github.com/hsoliwal/initiative31-eclipse.pde/commit/4454bcd7eb06e1fd6623a33151761e67310e8ca2).
There were no extra hsoliwal implementation commits in that comparison. This
reader guide and cheat sheet add documentation; inherited fixes and tests remain
the work of their original authors.

The repository description calls this a fork for an API Tools bug. That is a
project-context clue, not evidence that a particular fix is complete, uniquely
implemented here, or part of an official Eclipse release. Check the exact branch,
commit, test and issue before making that claim. The Initiative 31 name likewise
does not by itself demonstrate a changed SWT backend or supported product.

## Learning the key concepts

| Term | Why it matters here |
| --- | --- |
| Plug-in / OSGi bundle | A component with identity, version and dependencies in `META-INF/MANIFEST.MF` |
| `Require-Bundle` | A dependency on another bundle |
| `Import-Package` | A dependency on exported packages; resolved wiring determines the provider |
| Target platform | The Eclipse bundles/features against which a workspace or build resolves |
| API baseline | A reference used to detect compatibility/versioning problems |
| Feature / p2 repository | Packaging and installation units for Eclipse components |
| PDE vs Tycho | PDE supplies development tooling; this repository's command-line build uses Maven/Tycho |

Read [Execution Environments](Execution_Environments.md) for bundle Java contracts
and [Source References](SourceReferences.md) for source provenance metadata.

## Finding the code

The root [pom.xml](../pom.xml) identifies these module groups:

| Path | Purpose | First investigation |
| --- | --- | --- |
| [apitools](../apitools) | API analysis, annotations and regression tests | Compatibility/baseline diagnostics |
| [ui](../ui) | PDE core models, editors, launching, tests and inspection tools | Manifest, dependency or plug-in development behavior |
| [ds](../ds) | Declarative Services tooling | Component description tooling |
| [e4tools](../e4tools) | Eclipse 4 development tools | Application-model tooling |
| [build](../build) | PDE Build sources | Existing PDE build/export behavior |
| [ua](../ua) | User-assistance components | Help integration |
| [features](../features) | Feature definitions | Packaged component membership |
| [org.eclipse.pde.doc.user](../org.eclipse.pde.doc.user) | PDE help content | End-user documentation |
| [repository](../repository) | p2 repository output | Included by the individual-bundles profile |

In [ui/pom.xml](../ui/pom.xml), `org.eclipse.pde.core`, `org.eclipse.pde.ui` and
`org.eclipse.pde.ui.tests` are separate reactor members. Some bundles are pomless:
read their manifest and `build.properties` as well as the containing POM.
Do not infer that a source bundle is absent merely because it lacks a local POM.

## Preparing the build

Use **JDK 25** for a setup aligned with the inspected [Jenkinsfile](../Jenkinsfile).
Use a current **Maven 3.9.x** installation; **3.9.16** is selected by the inspected
4.42 aggregator reusable workflow, while this PDE checkout requests the
**4.41.0-SNAPSHOT** parent. That workflow version is a practical starting point,
not a derived minimum for the older PDE parent.

Important version distinctions at this snapshot:

- [.mvn/extensions.xml](../.mvn/extensions.xml) selects Tycho **5.0.1**
- [pom.xml](../pom.xml) requests `eclipse-platform-parent:4.41.0-SNAPSHOT`
- [.mvn/maven.config](../.mvn/maven.config) enables `build-individual-bundles`,
  preserves stack traces, and sets `tycho.localArtifacts=ignore`
- The original README's Maven 3.3.1 statement and older Java/Tycho details in
  `AGENTS.md` should not be mistaken for the current CI toolchain
- CI calls the upstream reusable workflow at moving `@master`, so inspect its
  resolved revision when reproducing a particular CI run

The parent profile provides an Eclipse-hosted repository for standalone parent-POM
resolution. A sibling aggregator checkout is therefore not inherently required for
an ordinary networked component build. You still need that exact parent snapshot
and the configured target platform to resolve. Do not substitute a 4.42 parent for
4.41 just to bypass an error.

Install Git, the selected JDK/Maven and native UI dependencies appropriate to your
host. Network access is required for uncached Maven/p2 artifacts. GUI tests need a
usable display; the PDE UI test POM explicitly enables the UI harness and UI thread.
BREE/API/Javadoc/TCK profiles bring additional toolchain or baseline requirements.

```sh
java -version
mvn -version
```

The latter shows which JVM launches Maven. It can differ from your interactive
`java` or a bundle's declared execution environment.

## Running a focused dependency-cycle test

This example explores code already present in the inherited source. It is not a
claim of a new fork-specific fix.

```sh
git clone https://github.com/hsoliwal/initiative31-eclipse.pde.git
cd initiative31-eclipse.pde
git rev-parse HEAD
mvn -version

mvn verify -Pbuild-individual-bundles \
  -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests \
  -Dtest=org.eclipse.pde.core.tests.internal.core.builders.DependencyLoopFinderTest
```

The [test source](../ui/org.eclipse.pde.ui.tests/src/org/eclipse/pde/core/tests/internal/core/builders/DependencyLoopFinderTest.java)
contains cases for direct and three-bundle cycles, package-import cycles,
Require-Bundle/Import-Package mixtures, alternate paths and declaration order.
The [test bundle POM](../ui/org.eclipse.pde.ui.tests/pom.xml) normally selects
`AllPDETests`; the `-Dtest` selector narrows the run to this class.

On Linux, if Xvfb is installed, use the same command through a virtual X11 display:

```sh
GDK_BACKEND=x11 xvfb-run -a mvn verify -Pbuild-individual-bundles \
  -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests \
  -Dtest=org.eclipse.pde.core.tests.internal.core.builders.DependencyLoopFinderTest
```

This includes the production core and test bundle so a core edit can participate
in the reactor. Add any other production bundle you change. Tests-only builds can
resolve production artifacts from the target instead; a passing test then does
not prove your edited production source was used.

Expect output beneath `ui/org.eclipse.pde.ui.tests/target/surefire-reports` after a
successful test execution. Verify the named class, test count, failures and skips.
A successful Maven command with no matching tests is not a useful regression result.

## Exploring the example in source

```sh
git grep -n 'testImportPackageCycle' -- ui/org.eclipse.pde.ui.tests
git grep -n 'testCycleMixingRequireBundleAndImportPackage' -- ui/org.eclipse.pde.ui.tests
git grep -n 'class DependencyLoopFinder' -- ui
git grep -n 'getResolvedImports' -- ui/org.eclipse.pde.core
```

Read the manifest setup in the regression test before inventing a new reproducer.
A declared package import and a resolved provider are different facts. A useful
bug report includes both the manifests and the target platform needed to reproduce
the wiring. Keep target/workspace fixtures disposable; these tests intentionally
create and remove test projects and restore target definitions.

## Building the broader PDE component

From the root:

```sh
mvn clean verify
```

This builds the PDE reactor, including the `repository` module activated by the
individual-bundles profile. It does not assemble a complete Eclipse SDK.
For the CI-requested TCK profile, after satisfying its prerequisites:

```sh
mvn clean verify -Ptck
```

The upstream [SDK aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator)
is a separate cross-project build. Its current parent and gitlinks may differ
from this fork's 4.41 source line. Combining them is an explicit integration task,
not a requirement to silently replace this fork's parent or dependencies.

The PDE p2 output is expected at `repository/target/repository` after a successful
applicable build. Local output is not an official signed/published Eclipse build.
Do not copy deployment or signing stages from Jenkins into a local quick start.

## Investigating API Tools

Start with [API Tools documentation](API_Tools.md), the code under `apitools/`, and
its [test configuration](../apitools/org.eclipse.pde.api.tools.tests/pom.xml).
The configured API Tools test bundle includes `ApiToolsPluginTestSuite` and
`ApiToolsTestSuite` and requires additional target-platform components.

```sh
git grep -n 'ApiToolsTestSuite' -- apitools
git grep -n 'ApiToolsPluginTestSuite' -- apitools
mvn verify -Pbuild-individual-bundles \
  -pl :org.eclipse.pde.api.tools,:org.eclipse.pde.api.tools.tests
```

Include any additional changed bundle in the selection. Record the exact API
baseline, execution environment, affected declaration and diagnostic before
concluding a compatibility warning is wrong. `-Papi-check` activates analysis when
the required baseline is available; it is not a substitute for a targeted test.

## Troubleshooting

| Symptom | Check | Interpretation |
| --- | --- | --- |
| Parent POM resolution fails | 4.41 snapshot, individual-bundles profile, Eclipse repository and Maven settings | A resolution problem, not proof standalone builds are unsupported |
| Unsupported Java/class version | `mvn -version`, JDK 25 selection and current Tycho/parent | Old README/agent notes may describe older tooling |
| SWT/no display failure | UI harness, native libraries and Xvfb/display | UI tests require an actual display environment |
| Test passes but edit seems absent | Selected production bundle, target resolution, baseline replacement | Tests may have exercised a target artifact |
| No tests run | Use `verify`, selector spelling and report contents | `mvn test` alone is not the Tycho verification route |
| API errors or missing `@since` | Baseline report and API/version policy | A compatibility contract to investigate, not a nuisance to suppress |
| p2 output absent | Profile/module selection and failed earlier reactor modules | Packaging may not have run |
| Stale local artifact confusion | `tycho.localArtifacts=ignore` and current target inputs | Installing a bundle locally does not guarantee it will be consumed |

Do not delete all of `~/.m2` as the first recovery step. Keep the first relevant
error, identify the failing coordinate and retry only after addressing its cause.

## Contributing and preserving attribution

Use this fork's [issues](https://github.com/hsoliwal/initiative31-eclipse.pde/issues)
for fork-specific reports and [pull requests](https://github.com/hsoliwal/initiative31-eclipse.pde/pulls)
for proposed fork changes. Read [CONTRIBUTING.md](../CONTRIBUTING.md) before upstream
contributions; its ECA and sign-off requirements still matter. Do not add an
agreement or sign-off on another person's behalf.

A strong report includes the source SHA, parent line, tool versions, target/baseline,
full command, minimal manifests or fixture, expected/actual diagnostic and actual
test report. Keep upstream authorship, [LICENSE](../LICENSE), [NOTICE](../NOTICE)
and file-level notices intact.

## Evidence and maintenance

The source snapshot is
[`4454bcd7eb06`](https://github.com/hsoliwal/initiative31-eclipse.pde/tree/4454bcd7eb06e1fd6623a33151761e67310e8ca2),
reviewed on **2026-10-14**. Tool selections come from the linked checked-in POMs,
Maven configuration and Jenkinsfile; test examples point to existing test sources.
Commands were source-checked but builds/tests were **not executed** for this
documentation update. Recheck versions and the immediate-parent comparison when
updating these notes.
