# Git Bash aplinka curl demonstracijai (paleidzia 3-curl-komandos.cmd).
# Prisijungia demo naudotojais ir paruosia antrastes, kurias naudoja docs/visual/DEMO_KOMANDOS.txt:
#   $A - rokas (DM ir savininkas grupese 1, 2), $J - jonas (zaidejas), $ADM - admin.

[ -f /etc/profile ] && . /etc/profile
[ -f ~/.bashrc ] && . ~/.bashrc

prisijungti() {
  curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"Demo123!\"}" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4
}

zetonai() {
  TOKEN=$(prisijungti rokas)
  if [ -z "$TOKEN" ]; then
    echo "API neatsako. Paleisk 1-paleisti-api, palauk, kol atsidarys Swagger, ir parasyk: zetonai"
    return 1
  fi
  A="Authorization: Bearer $TOKEN"
  J="Authorization: Bearer $(prisijungti jonas)"
  ADM="Authorization: Bearer $(prisijungti admin)"
  echo "Prisijungta: \$A - rokas, \$J - jonas, \$ADM - admin."
  echo "Zetonai galioja 15 min. Gavus 401 \"Access zetonas pasibaiges\", parasyk: zetonai"
}

# JWT turinys (vidurine dalis, Base64url): jwt - roko zetonas, jwt "$kitas_zetonas" - bet kuris
jwt() {
  local p
  p=$(printf '%s' "${1:-$TOKEN}" | cut -d. -f2 | tr '_-' '/+')
  while [ $(( ${#p} % 4 )) -ne 0 ]; do p="$p="; done
  printf '%s' "$p" | base64 -d
  echo
}

echo "Done-n-Dusted LD2 - curl demonstracija. Komandos: docs/visual/DEMO_KOMANDOS.txt (atidaryta Notepad)."
zetonai && echo "JWT turinys: jwt   |   Nauji zetonai: zetonai"
