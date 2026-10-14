# PDE contributor orientation and target-platform walkthrough

Eclipse PDE is tooling for developing, launching, documenting and checking Eclipse
plug-ins and OSGi-based applications. This fork contains the PDE source tree; it is
not itself a plug-in project template or a complete standalone Eclipse SDK.
The [README](../README.md) preserves the upstream introduction and contribution route.

## Find the owner

| Area | Source | Documentation |
| --- | --- | --- |
| Plug-in models, editors, launches and tests | [ui](../ui) | [User guide](User_Guide.md), [FAQ](FAQ.md) |
| API compatibility and baseline analysis | [apitools](../apitools) | [API Tools](API_Tools.md) |
| Declarative Services tooling | [ds](../ds) | [User guide](User_Guide.md) |
| Eclipse 4 tooling | [e4tools](../e4tools) | [User guide](User_Guide.md) |
| PDE Build and exported applications | [build](../build) | [Execution environments](Execution_Environments.md) |
| User assistance and packaged docs | [ua](../ua), [org.eclipse.pde.doc.user](../org.eclipse.pde.doc.user) | [Source references](SourceReferences.md) |

The [root reactor](../pom.xml) also includes features. Its
`build-individual-bundles` profile adds the p2 repository module; do not assume that
a directory's presence means it participates in every Maven profile.

## Current build inputs

Use JDK 25 to match the current [Jenkinsfile](../Jenkinsfile), rather than the older
JDK 17/21 guidance. The [Tycho extension](../.mvn/extensions.xml) is 5.0.1; the
[root POM](../pom.xml) requests eclipse-platform-parent 4.41.0-SNAPSHOT.
Use a current Maven 3.9.x installation compatible with that toolchain and confirm
the actual Java/Maven selection using `mvn -version`; the Jenkinsfile names
`apache-maven-latest` rather than pinning an exact Maven release.

[Default options](../.mvn/maven.config) activate `build-individual-bundles` and set
`tycho.localArtifacts=ignore`. The profile can resolve the matching parent from
Eclipse's Maven repository when no sibling parent exists. Standalone resolution
therefore depends on that snapshot being available; it is not guaranteed to fail
merely because this is a standalone checkout. Network access to Maven and p2
repositories is needed on a cold cache.

The [PDE UI test POM](../ui/org.eclipse.pde.ui.tests/pom.xml) explicitly uses a UI
harness and UI thread. Provide a graphical session or a configured virtual display.
Keep the target platform (bundles used to build/run) separate from the IDE you use
to edit the source.

## Cheat sheet

Commands below are source-checked but unexecuted for this documentation change.
Run them from the repository root.

```sh
mvn -version
git status --short
# Run a focused target-persistence test against this checkout's PDE core.
mvn verify -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests -Pbuild-individual-bundles -Dtest=TargetDefinitionPersistenceTests
# Run the full PDE UI test bundle with the core source selected.
mvn verify -pl :org.eclipse.pde.core,:org.eclipse.pde.ui.tests -Pbuild-individual-bundles
# Broader validation after configuring the target, display and BREE toolchains.
mvn clean verify -Pbree-libs -Papi-check -Pjavadoc -Ptck
```

Use `verify`, not just `test`, for Tycho's integration-test lifecycle.
Inspect the selected test bundle's `target/surefire-reports`, compiler logs and API
analysis reports. The p2 output is `repository/target/repository` when that module
is active and succeeds. CI's test-failure-ignore and deployment steps are not
appropriate defaults for a local correctness check.

## Worked exploration: a target definition is not a resolved target

1. Read [Target Definitions](Target_Definitions.md) to distinguish an editable
   `.target` definition from the active target platform.
2. Open [ITargetDefinition](../ui/org.eclipse.pde.core/src/org/eclipse/pde/core/target/ITargetDefinition.java).
   Follow `resolve`, `isResolved`, `getBundles` and `getAllBundles`.
   Resolution returns statuses, including cancellation; a resolved container may
   still contain bundles with problem statuses. `getBundles` applies inclusion
   filters, while `getAllBundles` describes the unfiltered resolved set.
3. Open [TargetDefinitionPersistenceTests](../ui/org.eclipse.pde.ui.tests/src/org/eclipse/pde/ui/tests/target/TargetDefinitionPersistenceTests.java).
   The memento tests distinguish restoring a handle from proving that its target
   exists. Follow the tests for reading/writing definitions before changing XML
   persistence.
4. Compare the neighboring [target tests](../ui/org.eclipse.pde.ui.tests/src/org/eclipse/pde/ui/tests/target),
   especially persistence versus resolution tests. A successful serialization
   round trip does not prove that an external p2 repository can resolve.
5. When reporting a problem, include the minimal public target definition,
   selected environment, repository URLs, source revision and resolution status.
   Remove credentials and private repository details before publishing it.

A local test selection establishes only its executed cases. API baseline checks,
target resolution, UI launches and performance claims require their own evidence.
Preserve existing notices and API compatibility; consult [API Tools](API_Tools.md)
and [CONTRIBUTING](../CONTRIBUTING.md) before proposing API changes.
