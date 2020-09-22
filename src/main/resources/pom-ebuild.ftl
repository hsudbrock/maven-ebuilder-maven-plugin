# Copyright 1999-2020 Gentoo Authors
# Distributed under the terms of the GNU General Public License v2

# This ebuild was generated using the java-ebuilder-maven-plugin.

EAPI=7

inherit maven-pom

DESCRIPTION="POM for: ${description}"
HOMEPAGE="${homepage}"
SRC_URI="${src_uri}"

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

src_unpack() {
	cp "${r"${DISTDIR}"}"/${pomFileName} "${r"${S}"}"/pom.xml || die "Could not copy downloaded pom file from ${r"${DISTDIR}"} to ${r"${S}"}"
}
