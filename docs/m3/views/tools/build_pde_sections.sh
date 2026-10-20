#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# PDE form sections on the pde-model adapter: apply recipe PdeSectionDeltas twice (second pass UNCHANGED), then compile
# the adapter and the changed sections against the stock PDE jars, the JFace candidate (org.eclipse.jface.viewers.m3)
# and the platform jars. Output under S:/m3-scratch/platform-views/pde. Run under the build lock:
#   bash C:/m3-superset/tools/build_lock.sh platform-views bash .../build_pde_sections.sh [norecipe]
set -e
PV=S:/m3-scratch/worktrees/eclipse.platform/docs/m3/platform-views/tools
. "$PV/pv_env.sh"
PDE=S:/m3-scratch/worktrees/eclipse.pde
U=$PDE/ui/org.eclipse.pde.ui/src/org/eclipse/pde/internal/ui/editor
D=$PDE/ds/org.eclipse.pde.ds.ui/src/org/eclipse/pde/internal/ds/ui/editor/sections
FILES=("$U/plugin/LibrarySection.java" "$U/plugin/ExecutionEnvironmentSection.java" "$D/DSPropertiesSection.java"
  "$D/DSProvideSection.java" "$D/DSReferenceSection.java" "$U/plugin/ExportPackageVisibilitySection.java"
  "$U/feature/IncludedFeaturesSection.java" "$U/plugin/RequiresSection.java" "$U/product/AbstractProductContentSection.java"
  "$U/build/BuildClasspathSection.java" "$U/build/RuntimeInfoSection.java" "$U/category/RepositoryReferenceSection.java"
  "$U/product/PropertiesSection.java" "$U/product/UpdatesSection.java" "$U/plugin/ExtensionsSection.java")
if [ "$1" != norecipe ]; then
  EXTRA_RECIPE_DIR=$PDE/docs/m3/views/recipe bash "$PV/apply_recipe.sh" com.synexia.openrewrite.platformviews.PdeSectionDeltas "${FILES[@]}"
fi
J=S:/m3-scratch/platform-views/jars
PJ="$J/org.eclipse.pde.launching-3.14.0.jar;$J/org.eclipse.ui.genericeditor-1.4.100.jar;$J/pdebuild-3.12.1000.jar;$J/org.eclipse.pde.ui-3.16.600.jar;$J/org.eclipse.pde.core-3.21.400.jar;$J/org.eclipse.pde.ds.ui-1.4.300.jar;$J/org.eclipse.pde.ds.core-1.3.900.jar"
EXTRA=""
for a in org.eclipse.ui.forms org.eclipse.ui.workbench.texteditor org.eclipse.ui.editors org.eclipse.core.filebuffers \
         org.eclipse.e4.ui.workbench org.eclipse.e4.core.contexts org.eclipse.ui.views org.eclipse.search org.eclipse.ltk.core.refactoring; do
  EXTRA="$EXTRA;$(pv_jar $a)"
done
for a in org.eclipse.jdt.core org.eclipse.jdt.ui org.eclipse.jdt.launching org.eclipse.jdt.debug; do
  v=$(ls "$M2/org/eclipse/jdt/$a" | grep -E '^[0-9]' | sort -V | tail -1); EXTRA="$EXTRA;$M2/org/eclipse/jdt/$a/$v/$a-$v.jar"
done
OUTP=$OUT/pde/classes
rm -rf "$OUTP" && mkdir -p "$OUTP"
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -nowarn -proc:none -g \
  -cp "$JFACE_CAND;$PJ;$STOCK_SWT;$DEPS$EXTRA;$STOCK_JFACE" -d "$OUTP" \
  "$U/m3/M3ModelChangedDeltas.java" "${FILES[@]}" 2>&1 | grep -E "error|warning: \[" | head -60 || true
echo "pde classes: $(find "$OUTP" -name '*.class' | wc -l)"
TESTS=$OUT/pde/tests
rm -rf "$TESTS" && mkdir -p "$TESTS"
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -nowarn -proc:none -g \
  -cp "$OUTP;$JFACE_CAND;$PJ;$STOCK_SWT;$DEPS$EXTRA;$STOCK_JFACE" -d "$TESTS" \
  "$PDE"/ui/org.eclipse.pde.ui.tests/src/org/eclipse/pde/ui/tests/m3/*.java 2>&1 | grep -E "error" | head -30 || true
echo "pde test classes: $(find "$TESTS" -name '*.class' | wc -l)"
echo "$OUTP;$JFACE_CAND;$PJ;$STOCK_SWT;$DEPS$EXTRA;$STOCK_JFACE" > "$OUT/pde/classpath.txt"
