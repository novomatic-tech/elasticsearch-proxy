#!/bin/sh

set -e

PUBLICATION_BRANCH=master
PUBLICATION_REPO=novomatic-tech/helm-charts-repo
PUBLICATION_DIR=$PWD/target/chart

git clone --branch=$PUBLICATION_BRANCH https://${GITHUB_TOKEN}@github.com/$PUBLICATION_REPO publish 2>&1 > /dev/null
cd publish/charts
cp -r $PUBLICATION_DIR/* .
git add .
git config user.name  "Travis"
git config user.email "travis@travis-ci.org"
git commit -m "Add helm chart."
git push origin $PUBLICATION_BRANCH 2>&1 > /dev/null