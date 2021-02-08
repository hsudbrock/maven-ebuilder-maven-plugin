# Copyright 1999-2021 Gentoo Authors
# Distributed under the terms of the GNU General Public License v2

# This ebuild was generated using the mvn-ebuilder-maven-plugin.

EAPI=7

inherit maven-pom

DESCRIPTION="TODO: POM for: ${description}"
HOMEPAGE="TODO: ${homepage}"
SRC_URI="${pom_src_uri}"

LICENSE="${license}"

SLOT="${version}"
KEYWORDS="amd64"

MAVEN_GROUP_ID="${groupId}"
MAVEN_ARTIFACT_ID="${artifactId}"
MAVEN_VERSION="${version}"

<#if parentPomAtom??>
BDEPEND="~${parentPomAtom}"

</#if>
S="${r"${WORKDIR}"}"