#!/usr/bin/env bash
#
# Runs the test suite across the JPA provider / database matrix and prints one
# result line per combination.
#
#   ./test-matrix.sh                          # the whole matrix
#   ./test-matrix.sh --db H2,POSTGRES         # only these databases
#   ./test-matrix.sh --provider HIBERNATE     # only this provider
#   ./test-matrix.sh --goal verify            # another maven goal (default: test)
#   ./test-matrix.sh -- -Dtest=TreeDataTest   # everything after -- goes to maven
#
# Every database except H2 starts a Testcontainers server, so Docker has to be
# running for those. Full logs land in target/test-matrix/<provider>-<database>.log.
#
set -uo pipefail

# the script lives at the project root, so run everything from there
cd "$(dirname "$0")"

PROVIDERS=(HIBERNATE ECLIPSELINK)
DATABASES=(H2 POSTGRES MARIADB MYSQL MSSQL ORACLE)
GOAL=test
MVN_ARGS=()

# ${x^^} needs bash 4, and macOS still ships 3.2
upper() { printf '%s' "$1" | tr '[:lower:]' '[:upper:]'; }

while [[ $# -gt 0 ]]; do
    case "$1" in
        --provider) IFS=',' read -r -a PROVIDERS <<< "$(upper "$2")"; shift 2 ;;
        --db|--database) IFS=',' read -r -a DATABASES <<< "$(upper "$2")"; shift 2 ;;
        --goal) GOAL="$2"; shift 2 ;;
        --) shift; MVN_ARGS=("$@"); break ;;
        -h|--help) awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "$0"; exit 0 ;;
        *) echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
    esac
done

LOG_DIR=target/test-matrix
mkdir -p "$LOG_DIR"

if [[ -t 1 ]]; then
    RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; RESET=$'\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; DIM=''; RESET=''
fi

results=()
failed=0

for provider in "${PROVIDERS[@]}"; do
    for database in "${DATABASES[@]}"; do
        label="$provider/$database"
        log="$LOG_DIR/$(echo "$provider-$database" | tr '[:upper:]' '[:lower:]').log"

        printf '%s>> %-24s%s' "$DIM" "$label" "$RESET"
        started=$SECONDS

        ./mvnw "$GOAL" \
            -Dtest.jpa.provider="$provider" \
            -Dtest.database="$database" \
            "${MVN_ARGS[@]+"${MVN_ARGS[@]}"}" >"$log" 2>&1
        status=$?
        elapsed=$((SECONDS - started))

        # the per-class lines end in "-- in <class>"; the aggregate one does not
        summary=$(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$log" | tail -1 | sed 's/^\[[A-Z]*\] *//')

        if [[ $status -eq 0 ]]; then
            printf '%sPASS%s  %s  %ss\n' "$GREEN" "$RESET" "${summary:-no tests reported}" "$elapsed"
            results+=("PASS|$label|${summary:-no tests reported}|${elapsed}s")
        elif [[ -n "$summary" ]]; then
            printf '%sFAIL%s  %s  %ss\n' "$RED" "$RESET" "$summary" "$elapsed"
            results+=("FAIL|$label|$summary|${elapsed}s")
            failed=1
        else
            # died before surefire produced a summary: no Docker, compile error, container timeout
            reason=$(grep -m1 -E '^\[ERROR\].*(Could not find a valid Docker|COMPILATION ERROR|Failed to execute goal)' "$log" \
                     | sed 's/^\[ERROR\] *//' | cut -c1-60)
            printf '%sERROR%s %s  %ss\n' "$YELLOW" "$RESET" "${reason:-see $log}" "$elapsed"
            results+=("ERROR|$label|${reason:-see $log}|${elapsed}s")
            failed=1
        fi
    done
done

echo
printf '%-8s %-24s %-52s %s\n' RESULT COMBINATION TESTS TIME
printf '%s\n' "$(printf '%.0s-' {1..96})"
for row in "${results[@]}"; do
    IFS='|' read -r outcome label summary elapsed <<< "$row"
    case "$outcome" in
        PASS)  colour=$GREEN ;;
        FAIL)  colour=$RED ;;
        *)     colour=$YELLOW ;;
    esac
    printf '%s%-8s%s %-24s %-52s %s\n' "$colour" "$outcome" "$RESET" "$label" "$summary" "$elapsed"
done
echo
echo "logs: $LOG_DIR/"

exit $failed
