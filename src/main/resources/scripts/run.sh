#!/usr/bin/env sh
set -eu

if test -f /var/run/secrets/nais.io/serviceuser/username
then
    export SERVICE_ACCOUNT_CLIENT_ID="$(cat /var/run/secrets/nais.io/serviceuser/username)"
    export SERVICE_ACCOUNT_CLIENT_SECRET="$(cat /var/run/secrets/nais.io/serviceuser/password)"
fi
