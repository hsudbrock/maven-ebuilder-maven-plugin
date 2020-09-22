# Copyright 1999-2020 Gentoo Authors
# Distributed under the terms of the GNU General Public License v2

# This ebuild was generated using the java-ebuilder-maven-plugin.

EAPI=7

inherit maven-bin

DESCRIPTION="Binary for: ${description}"
HOMEPAGE="${homepage}"
SRC_URI="${src_uri}"

LICENSE="${license}"

SLOT="${version}"
KEYWORDS="amd64"

MAVEN_GROUP_ID="${groupId}"
MAVEN_ARTIFACT_ID="${artifactId}"
MAVEN_VERSION="${version}"

S="${r"${WORKDIR}"}"

src_unpack() {
	cp "${r"${DISTDIR}"}"/${jarFileName} "${r"${S}"}"/${jarFileName} || die "Could not copy downloaded jar file from ${r"${DISTDIR}"} to ${r"${S}"}"
}
