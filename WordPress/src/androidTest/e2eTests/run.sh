#!/bin/bash
cd "$(dirname "$0")"
MOCHA=../../../../node_modules/mocha/bin/mocha
REPORTER=""
AFTER="lib/after.js"
OPTS=""
RETURN=0

declare -a TARGETS
declare -a ORIENTATIONS

# Hard coded to just the specs directory, will change if/when we add something else like visdiff or i18n tests
TARGETS+=("specs/")

usage () {
  cat <<EOF
-R	- Use custom Slack/Spec/XUnit reporter, otherwise just use Spec reporter
-p	- Portrait orientation
-l	- Landscape orientation
-h	- This help listing
EOF
  exit 0
}

if [ $# -eq 0 ]; then
  usage
fi

while getopts ":Rplh" opt; do
  case $opt in
    R)
      REPORTER="-R spec-xunit-slack-reporter"
      continue
      ;;
    p)
      ORIENTATIONS+=("PORTRAIT")
      continue
      ;;
    l)
      ORIENTATIONS+=("LANDSCAPE")
      continue
      ;;
    h)
      usage
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      usage
      ;;
    :)
      echo "Option -$OPTARG requires an argument" >&2
      echo ""
      usage
      ;;
  esac

  TARGETS+=("$TARGET")
done

if [ ${#ORIENTATIONS[@]} == 0 ]; then
  echo "Must supply at least one -p or -l" >&2
  echo ""
  usage
fi

for orientation in "${ORIENTATIONS[@]}"; do
  for target in "${TARGETS[@]}"; do
    CMD="env ORIENTATION=$orientation $MOCHA $REPORTER $target $AFTER"

    eval $CMD
    RETURN+=$?
  done
done

exit $RETURN
