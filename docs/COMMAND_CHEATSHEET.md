# PDE command and concept cheat sheet

[Full fork guide](FORK_GUIDE.md) · [README](../README.md) · [API Tools](API_Tools.md)

Run from the repository root. Use the guide's JDK/Maven and display prerequisites.
These commands are source-grounded examples, not claimed successful test results.

## Establish the source and tool versions

```sh
git status --short
git rev-parse HEAD
mvn -version
cat .mvn/extensions.xml
cat .mvn/maven.config
```

At the documented snapshot: parent 4.41.0-SNAPSHOT, Tycho extension 5.0.1, Jenkins
build JDK 25. Keep these distinct from individual bundle execution environments.

## Choose a test scope

```sh
# Existing dependency-cycle regression class, plus its production core.
mvn verify -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests \
  -Dtest=org.eclipse.pde.core.tests.internal.core.builders.DependencyLoopFinderTest

# PDE UI's configured suite, plus core and UI production bundles.
mvn verify -pl :org.eclipse.pde.core,:org.eclipse.pde.ui,:org.eclipse.pde.ui.tests

# API Tools implementation and its configured test suites.
mvn verify -pl :org.eclipse.pde.api.tools,:org.eclipse.pde.api.tools.tests

# Complete PDE component reactor.
mvn clean verify
```

`.mvn/maven.config` activates `-Pbuild-individual-bundles`. Use it explicitly if
your invocation does not pick up that configuration. Add every production bundle
you changed; do not assume tests-only selection recompiles it. Use `verify` for
Tycho's integration-test lifecycle, and inspect actual executed tests.

## Useful options

| Option | Purpose | Caution |
| --- | --- | --- |
| `-Dtest=fully.qualified.TestClass` | Select a focused test | Verify the class exists and the report contains it |
| `-Papi-check` | API compatibility analysis | Requires the configured baseline |
| `-Pjavadoc` | Javadoc checking/generation | Has toolchain/documentation prerequisites |
| `-Ptck` | Profile requested by PDE CI | Not needed to read the source or run every focused test |
| `-Pbree-libs` | Execution-environment library toolchains | Configure the required installed JDKs first |
| `-DskipTests` | Skip test execution during a build | No test evidence |
| `--fail-at-end` | Continue independent reactor work after failures | The final build can still fail |

Linux UI test runs may be prefixed with `GDK_BACKEND=x11 xvfb-run -a` when Xvfb is
installed. This does not provide missing native libraries or target-platform data.

## Find the contract or test

```sh
git grep -n 'class DependencyLoopFinder' -- ui
git grep -n 'testImportPackageCycle' -- ui/org.eclipse.pde.ui.tests
git grep -n 'ApiToolsTestSuite' -- apitools
git grep -n 'Bundle-RequiredExecutionEnvironment' -- '*MANIFEST.MF'
git ls-files '*MANIFEST.MF' '*build.properties' '*pom.xml'
```

| Need | Start |
| --- | --- |
| Manifest/dependency model | `ui/org.eclipse.pde.core` |
| Editors and UI | `ui/org.eclipse.pde.ui` |
| Dependency-cycle test | `ui/org.eclipse.pde.ui.tests/src/org/eclipse/pde/core/tests/internal/core/builders/DependencyLoopFinderTest.java` |
| API compatibility tooling | `apitools/` and `docs/API_Tools.md` |
| Target-platform concepts | `docs/Target_Definitions.md` |
| Help content | `org.eclipse.pde.doc.user/` |

## Find build evidence

```sh
find . -path '*/target/surefire-reports/*' -type f
find . -path '*/target/apianalysis/*' -type f
find . -path '*/target/compilelogs/*' -type f
find repository/target/repository -maxdepth 1 -type f
```

Inspect timestamps, test counts, failures and skips. The output repository exists
only after the applicable build completes. It is not the full Eclipse SDK.

## Compare with the immediate parent

In a fresh clone, add the remote once; first inspect `git remote -v` in an existing
checkout so you do not overwrite a remote someone already configured.

```sh
git remote add initiative31 https://github.com/swt-initiative31/eclipse.pde.git
git fetch initiative31 master
git log --oneline --left-right initiative31/master...HEAD
git diff --stat initiative31/master...HEAD
```

These fetch/inspection commands do not merge. The three-dot diff shows the fork
side since the merge base. For original-project history, the upstream project is
`eclipse-pde/eclipse.pde`; do not confuse it with the immediate parent.

## A reproducible report

Include source SHA, OS/architecture, `mvn -version`, target platform/API baseline,
production and test selectors, full command, minimal manifests, expected/actual
behavior and the relevant report. Review logs for secrets before posting publicly.
