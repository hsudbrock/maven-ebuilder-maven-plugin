# Copyright 1999-2021 Gentoo Authors
# Distributed under the terms of the GNU General Public License v2

# This ebuild was generated using the mvn-ebuilder-maven-plugin.

EAPI=7

inherit maven-bin

DESCRIPTION="TODO: ${description}"
HOMEPAGE="TODO: ${homepage}"
SRC_URI="${jar_src_uri}
         ${pom_src_uri}"

LICENSE="${license}"

SLOT="${version}"
KEYWORDS="~amd64"

MAVEN_GROUP_ID="${groupId}"
MAVEN_ARTIFACT_ID="${artifactId}"
MAVEN_VERSION="${version}"

S="${r"${WORKDIR}"}"

BDEPEND="
<#list buildDependencies as dependency>
         ${dependency}
</#list>
        "