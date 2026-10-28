#!/usr/bin/env bash
#
# Runs the test suite across the JPA version / provider / database matrix and prints one
# result line per combination.
#
#   ./test-matrix.sh                          # the whole matrix
#   ./test-matrix.sh --db H2,POSTGRES         # only these databases
#   ./test-matrix.sh --provider HIBERNATE     # only this provider
#   ./test-matrix.sh --jpa 3.0,3.2            # only these Jakarta Persistence versions
#                                             # (3.0-hibernate5: the Java 8 Hibernate line)
#   ./test-matrix.sh --goal verify            # another maven goal (default: test)
#   ./test-matrix.sh --json docs/docs/compatibility-matrix.json
#                                             # also write the results for the docs page
#   ./test-matrix.sh -- -Dtest=TreeDataTest   # everything after -- goes to maven
#
# Every database except H2 starts a Testcontainers server, so Docker has to be
# running for those. Full logs land in target/test-matrix/<jpa>-<provider>-<database>.log.
#
set -uo pipefail

# the script lives at the project root, so run everything from there
cd "$(dirname "$0")"

JPA_VERSIONS=(3.0 3.0-hibernate5 3.1 3.2)
PROVIDERS=(HIBERNATE ECLIPSELINK)
DATABASES=(H2 POSTGRES MARIADB MYSQL MSSQL ORACLE)
GOAL=test
MVN_ARGS=()
JSON_OUT=''

# ${x^^} needs bash 4, and macOS still ships 3.2
upper() { printf '%s' "$1" | tr '[:lower:]' '[:upper:]'; }

json_str() {
    local s=${1//\\/\\\\}
    s=${s//\"/\\\"}
    s=${s//$'\t'/ }
    printf '"%s"' "$s"
}

# one JSON object per combination: versions from the MATRIX-ENV line TestPersistence prints,
# counts from the surefire summary, failing tests from its Failures/Errors lists
json_combination() {
    local jpa=$1 provider=$2 database=$3 outcome=$4 log=$5 summary=$6
    local env tests failures errors skipped failed='' entry name message
    env=$(grep -m1 '^MATRIX-ENV|' "$log")
    IFS='|' read -r _ _ provider_version db_product db_version java_version <<< "$env"
    tests=$(sed -nE 's/.*Tests run: ([0-9]+).*/\1/p' <<< "$summary")
    failures=$(sed -nE 's/.*Failures: ([0-9]+).*/\1/p' <<< "$summary")
    errors=$(sed -nE 's/.*Errors: ([0-9]+).*/\1/p' <<< "$summary")
    skipped=$(sed -nE 's/.*Skipped: ([0-9]+).*/\1/p' <<< "$summary")
    while IFS= read -r entry; do
        entry=${entry#\[ERROR\]   }
        # "Class.method:12 » Exception message", or "Class>Base.setUp:43 » ..." when the whole class failed
        name=$(sed -E 's/^([A-Za-z0-9_]+)>.*/\1/; s/^([A-Za-z0-9_]+\.[A-Za-z0-9_]+).*/\1/' <<< "$entry")
        message='assertion failed'
        if [[ "$entry" == *' » '* ]]; then
            message=$(cut -c1-300 <<< "${entry#* » }")
        fi
        failed+="${failed:+,}{\"test\":$(json_str "$name"),\"message\":$(json_str "$message")}"
    done < <(grep -E '^\[ERROR\]   [A-Z][A-Za-z0-9_]*[.>]' "$log")
    printf '{"jpa":%s,"provider":%s,"providerVersion":%s,"database":%s,"databaseProduct":%s,"databaseVersion":%s,"java":%s,"outcome":%s,"tests":%s,"failures":%s,"errors":%s,"skipped":%s,"failed":[%s]}' \
        "$(json_str "$jpa")" "$(json_str "$provider")" "$(json_str "$provider_version")" "$(json_str "$database")" \
        "$(json_str "$db_product")" "$(json_str "$db_version")" "$(json_str "$java_version")" "$(json_str "$outcome")" \
        "${tests:-0}" "${failures:-0}" "${errors:-0}" "${skipped:-0}" "$failed"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --jpa) IFS=',' read -r -a JPA_VERSIONS <<< "$2"; shift 2 ;;
        --provider) IFS=',' read -r -a PROVIDERS <<< "$(upper "$2")"; shift 2 ;;
        --db|--database) IFS=',' read -r -a DATABASES <<< "$(upper "$2")"; shift 2 ;;
        --goal) GOAL="$2"; shift 2 ;;
        --json) JSON_OUT="$2"; shift 2 ;;
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
json_rows=()
failed=0

for jpa in "${JPA_VERSIONS[@]}"; do
    for provider in "${PROVIDERS[@]}"; do
        # that profile only swaps Hibernate, with EclipseLink it would repeat the 3.0 run
        if [[ "$jpa" == "3.0-hibernate5" && "$provider" != "HIBERNATE" ]]; then
            continue
        fi
        for database in "${DATABASES[@]}"; do
            label="$jpa/$provider/$database"
            log="$LOG_DIR/$(echo "$jpa-$provider-$database" | tr '[:upper:]' '[:lower:]').log"

            printf '%s>> %-34s%s' "$DIM" "$label" "$RESET"
            started=$SECONDS

            # 3.0 matches no profile and runs the baseline
            ./mvnw "$GOAL" \
                -Djpa="$jpa" \
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
            if [[ -n "$JSON_OUT" ]]; then
                json_rows+=("$(json_combination "$jpa" "$provider" "$database" "${results[${#results[@]}-1]%%|*}" "$log" "$summary")")
            fi
        done
    done
    done

    echo
    printf '%-8s %-34s %-52s %s\n' RESULT COMBINATION TESTS TIME
    printf '%s\n' "$(printf '%.0s-' {1..102})"
    for row in "${results[@]}"; do
        IFS='|' read -r outcome label summary elapsed <<< "$row"
        case "$outcome" in
            PASS)  colour=$GREEN ;;
            FAIL)  colour=$RED ;;
            *)     colour=$YELLOW ;;
        esac
        printf '%s%-8s%s %-34s %-52s %s\n' "$colour" "$outcome" "$RESET" "$label" "$summary" "$elapsed"
done
echo
echo "logs: $LOG_DIR/"

if [[ -n "$JSON_OUT" ]]; then
    {
        printf '{\n  "generatedAt": %s,\n  "commit": %s,\n  "combinations": [\n' \
            "$(json_str "$(date -u +%Y-%m-%d)")" "$(json_str "$(git describe --always --dirty 2>/dev/null)")"
        for i in "${!json_rows[@]}"; do
            printf '    %s%s\n' "${json_rows[$i]}" "$([[ $i -lt $((${#json_rows[@]} - 1)) ]] && echo ,)"
        done
        printf '  ]\n}\n'
    } > "$JSON_OUT"
    echo "json: $JSON_OUT"
fi

exit $failed
