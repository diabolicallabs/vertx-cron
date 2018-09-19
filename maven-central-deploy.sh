#!/usr/bin/env bash

# Deploy maven artifact in current directory into Maven central repository
# using maven-release-plugin goals

read -p "Really deploy to maven central repository  (yes/no)? "

if ( [ "$REPLY" == "yes" ] ) then
  ssh-add ~/.ssh/id_rsa
  ssh-add -l
  mvn release:clean release:prepare release:perform -B -e | tee maven-central-deploy.log
  ssh-add -D
else
  echo 'Exit without deploy'
fi