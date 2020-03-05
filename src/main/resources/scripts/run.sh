#!/usr/bin/env sh
set -eu

if test -f /var/run/secrets/nais.io/serviceuser/username
then
    export SYSTEMBRUKER_USERNAME="$(cat /var/run/secrets/nais.io/serviceuser/username)"
    export SYSTEMBRUKER_PASSWORD="$(cat /var/run/secrets/nais.io/serviceuser/password)"
fi

