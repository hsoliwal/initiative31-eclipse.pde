# Working with this Initiative 31 PDE fork

[Repository README](../README.md) · [Upstream contribution guide](../CONTRIBUTING.md) · [PDE user guide](User_Guide.md)

## Which project is this?

Eclipse PDE is the Plug-in Development Environment: tools for developing, testing, debugging, building, and deploying Eclipse plug-ins and RCP applications. It is tooling around Eclipse projects; it is not an SWT widget implementation or the Initiative 31 demonstrator.

The repository lineage is:

1. [`eclipse-pde/eclipse.pde`](https://github.com/eclipse-pde/eclipse.pde), the original Eclipse PDE project
2. [`swt-initiative31/eclipse.pde`](https://github.com/swt-initiative31/eclipse.pde), whose public description identifies it as a PDE fork for an API Tools bug
3. [`hsoliwal/initiative31-eclipse.pde`](https://github.com/hsoliwal/initiative31-eclipse.pde), this fork

This guide describes the source at `4454bcd7eb06e1fd6623a33151761e67310e8ca2` on this fork's `master`, before the guide was added. It does not claim that this fork is identical to today's upstream or that an API Tools fix is present or accepted upstream. For Initiative 31's SWT research and implementation links, read the [final project report](https://github.com/swt-initiative31/documents/blob/main/report/overall_report.md).

## Find the right entry point

| You want to work on | Start here |
| --- | --- |
| PDE concepts and end-user tasks | [User Guide](User_Guide.md) and [FAQ](FAQ.md) |
| API compatibility, baselines, and API Tools | [API Tools guide](API_Tools.md) and [`apitools/`](../apitools/) |
| Target platforms and dependencies | [Target Definitions](Target_Definitions.md) and [Execution Environments](Execution_Environments.md) |
| Source provenance in built bundles | [Source References](SourceReferences.md) |
| Plug-in development UI and tests | [`ui/`](../ui/) and [UI test bundle POM](../ui/org.eclipse.pde.ui.tests/pom.xml) |
| Declarative Services or Eclipse 4 tooling | [`ds/`](../ds/) or [`e4tools/`](../e4tools/) |
| Build integration and module selection | [Root POM](../pom.xml), [`.mvn/`](../.mvn/), and [`build/`](../build/) |
| Help content or features | [`org.eclipse.pde.doc.user/`](../org.eclipse.pde.doc.user/), [`ua/`](../ua/), and [`features/`](../features/) |

The root POM lists eight top-level modules. Its `build-individual-bundles` profile additionally includes [`repository/`](../repository/). Use the POM for the actual reactor rather than assuming every root directory is independently runnable.

## Get the source and choose an IDE setup

A source-only checkout can begin with:

```sh
git clone --branch master https://github.com/hsoliwal/initiative31-eclipse.pde.git
cd initiative31-eclipse.pde
git branch --show-current
git rev-parse HEAD
git remote -v
```

Record the commit with a bug report or test result. Check the remote before pushing so a fork-specific change targets the intended repository.

The inherited README's Eclipse Installer button uses the official PDE Oomph setup. The checked-in [PDEConfiguration.setup](../releng/org.eclipse.pde.setup/PDEConfiguration.setup) selects the upstream PDE `master` stream and a latest Eclipse SDK product. It is useful for provisioning an Eclipse development workspace, but it does not automatically select this `hsoliwal` fork. Confirm the imported checkout, branch, and remotes before editing.

## Read the build configuration before choosing a toolchain

The following values were read from this snapshot, not from a completed build:

| Configuration | What the checked-in file says |
| --- | --- |
| [`pom.xml`](../pom.xml) | Parent `org.eclipse:eclipse-platform-parent:4.41.0-SNAPSHOT`, with relative path `../eclipse-platform-parent` |
| [`.mvn/extensions.xml`](../.mvn/extensions.xml) | Tycho build extension `5.0.1` |
| [`.mvn/maven.config`](../.mvn/maven.config) | Enables `build-individual-bundles`, keeps stack traces, and sets `tycho.localArtifacts=ignore` |
| [`Jenkinsfile`](../Jenkinsfile) | Selects `apache-maven-latest` and `temurin-jdk25-latest`, with an `xvnc` display for the build |
| [GitHub CI workflow](../.github/workflows/ci.yml) | Delegates to the Eclipse Platform reusable build workflow with `clean verify -Ptck` |

Some inherited prose, including version examples in [AGENTS.md](../AGENTS.md), refers to older parent, Tycho, and Java versions. For version selection, reconcile those notes with the current configuration files above. The Jenkins selection documents that pipeline's toolchain, not a separately established minimum JDK or Maven version.

The `build-individual-bundles` profile configures the Eclipse Maven repository for parent resolution. A missing sibling parent directory alone does not prove the build is impossible: Maven may resolve the parent remotely. Conversely, a configured repository does not guarantee a historical snapshot is still available. Diagnose the actual parent/artifact resolution error before treating it as a source compilation failure.

## Build and test entry points

After preparing a compatible toolchain, a resolvable parent/target platform, and a display when UI tests require it, the inherited repository build entry point is:

```sh
mvn clean verify
```

The checked-in GitHub CI additionally selects the TCK profile:

```sh
mvn clean verify -Ptck
```

For a narrower UI-test investigation, the repository instructions provide this bundle selector:

```sh
mvn verify -pl :org.eclipse.pde.ui.tests -Pbuild-individual-bundles
```

Use `verify` for the Tycho test lifecycle rather than assuming `mvn test` ran the integration tests. A focused test bundle run is not equivalent to the full reactor or CI profiles.

The [UI test POM](../ui/org.eclipse.pde.ui.tests/pom.xml) identifies `org.eclipse.pde.ui.tests.AllPDETests` and enables both `useUIHarness` and `useUIThread`. These tests need an appropriate GUI/display environment; “headless terminal” does not mean that a display-dependent test can run without one. Initial dependency/target-platform resolution also needs network access unless the required artifacts are already available.

The Jenkins build uses more profiles and environment-specific settings than the basic command above. Read its build stage for comparison; do not copy deployment, signing, or infrastructure credentials into a local recipe.

### Interpret results precisely

- **Resolution failed:** capture the missing parent, artifact, or target-platform location and the exact revision
- **Compilation failed:** preserve the first useful compiler error and relevant `target/compilelogs/` output
- **Tests failed:** inspect `target/surefire-reports/*.xml` under the affected projects and distinguish display/harness startup from assertion failures
- **API analysis failed:** inspect `target/apianalysis/*.xml` and the [API Tools guide](API_Tools.md)
- **Command succeeded:** check which bundles/tests and profiles actually ran before claiming full validation

The report paths above come from the Jenkins artifact and test-report configuration. This documentation change did not execute Maven, compile bundles, run GUI tests, or certify a build.

## Propose a useful contribution

For a fork-specific guide improvement, target this fork's `master` in a focused PR. For an Eclipse PDE change intended for upstream, follow [CONTRIBUTING.md](../CONTRIBUTING.md), including the Eclipse Contributor Agreement and contribution/sign-off requirements. Opening a fork PR does not mean a contribution has been accepted upstream.

Include the affected plug-in or document, exact commit, expected and actual result, relevant build/test command, and what remains unverified. Keep API compatibility and versioning implications visible for code changes. For documentation-only work, check relative links and heading fragments, review the final changed-file list, and use `git diff --check` before submission.

Retain [LICENSE](../LICENSE), [NOTICE](../NOTICE), source headers, and contributor credits. The notice identifies EPL-2.0 and the original authorship/trademarks; this guide does not replace per-file notices or change licensing.
