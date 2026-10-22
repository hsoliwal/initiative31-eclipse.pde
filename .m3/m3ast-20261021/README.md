<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original Eclipse PDE/OSGi source receipt

One exact original POM source recipe `com.synexia.eclipse.EstateAstInitiative31EclipsePde20261021`, Synexia `b77c892d7519c6aa037c2b5cebd8658867ea712b`.
The opt-in `m3-ast-source-audit-20261021` profile validates that source
identity without changing PDE's Tycho build, JDT original AST, Equinox
extension semantics or SWT/native API. No duplicate compiler/parser or
runtime M3+JDK layer is introduced.

Direct proof: `python3 .m3/m3ast-20261021/verify.py`. Actual PDE
compilation, JUnit, extension/UI runtime and JNI/HotSpot gates are NOT_RUN.
