import React from 'react';
import matrix from './compatibility-matrix.json';

interface Failure {
    test: string;
    message: string;
}

interface Combination {
    jpa: string;
    provider: string;
    providerVersion: string;
    database: string;
    databaseProduct: string;
    databaseVersion: string;
    java: string;
    outcome: string;
    tests: number;
    failures: number;
    errors: number;
    skipped: number;
    failed: Failure[];
}

interface KnownIssue {
    label: string;
    // heading id of the explanation in compatibility.md
    anchor: string;
    provider: string;
    // provider major.minor, any version when absent
    versions?: string[];
    databases?: string[];
    test: RegExp;
    message?: RegExp;
}

// keep in sync with the "Known issues" headings in compatibility.md; the first match explains a failure
const KNOWN_ISSUES: KnownIssue[] = [
    {
        label: 'A', anchor: 'eclipselink-case-grouping', provider: 'ECLIPSELINK',
        test: /^ComputedColumnTest\.(groupingByACaseExpression|pivotingGroupsAndSortsByAComputedColumn)$/,
    },
    {
        label: 'B', anchor: 'eclipselink-time-zone-types', provider: 'ECLIPSELINK', databases: ['MSSQL', 'ORACLE'],
        test: /^DateFactoryFilterTest\./,
    },
    {
        label: 'C', anchor: 'eclipselink-4-null-uuid', provider: 'ECLIPSELINK', versions: ['4.0'],
        test: /^(AdvancedFilterTest\.set\w+|SetFilterTest\.booleanSetFilter)$/, message: /NullPointerException/,
    },
    {
        label: 'D', anchor: 'eclipselink-3-uuid-comparison', provider: 'ECLIPSELINK', versions: ['3.0'], databases: ['MSSQL', 'ORACLE'],
        test: /^(SetFilterTest\.uuidSetFilter|AdvancedFilterTest\.setParsesValuesWithTheColumnsSetFilter)$/,
    },
    {
        // the test data cannot be inserted, so every test class fails while it sets up
        label: 'E', anchor: 'eclipselink-5-sql-server-uuid', provider: 'ECLIPSELINK', versions: ['5.0'], databases: ['MSSQL'],
        test: /^\w+Test$/,
    },
];

const PROVIDERS: Record<string, string> = {HIBERNATE: 'Hibernate', ECLIPSELINK: 'EclipseLink'};

// the Java version each provider major version needs
const JAVA_MINIMUM: Record<string, string> = {
    'HIBERNATE 5': '8', 'HIBERNATE 6': '11', 'HIBERNATE 7': '17',
    'ECLIPSELINK 3': '8', 'ECLIPSELINK 4': '11', 'ECLIPSELINK 5': '17',
};

const DATABASES: { key: string; name: string }[] = [
    {key: 'H2', name: 'H2'},
    {key: 'POSTGRES', name: 'PostgreSQL'},
    {key: 'MARIADB', name: 'MariaDB'},
    {key: 'MYSQL', name: 'MySQL'},
    {key: 'MSSQL', name: 'SQL Server'},
    {key: 'ORACLE', name: 'Oracle'},
];

// SQL Server reports its engine version, which nobody calls it by
const SQL_SERVER_RELEASES: Record<string, string> = {'15': '2019', '16': '2022', '17': '2025'};

const styles: Record<string, React.CSSProperties> = {
    table: {
        fontSize: '0.875rem',
    },
    cell: {
        textAlign: 'center',
        whiteSpace: 'nowrap',
    },
    provider: {
        fontWeight: 600,
        verticalAlign: 'top',
    },
    version: {
        whiteSpace: 'nowrap',
    },
    details: {
        display: 'block',
        fontSize: '0.75rem',
        color: 'var(--ifm-color-emphasis-700)',
    },
    databaseVersion: {
        display: 'block',
        fontSize: '0.75rem',
        fontWeight: 'normal',
        color: 'var(--ifm-color-emphasis-700)',
    },
    label: {
        marginLeft: '0.2rem',
        fontWeight: 600,
    },
    caption: {
        fontSize: '0.8rem',
        color: 'var(--ifm-color-emphasis-700)',
        marginTop: '-0.5rem',
        marginBottom: '1rem',
    },
    unexplained: {
        border: '1px solid var(--ifm-color-warning-dark)',
        borderLeft: '4px solid var(--ifm-color-warning)',
        borderRadius: '6px',
        padding: '0.75rem 1rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
    },
};

const majorMinor = (version: string) => version.split('.').slice(0, 2).join('.');

function explain(combination: Combination, failure: Failure): KnownIssue | undefined {
    return KNOWN_ISSUES.find(issue =>
        issue.provider === combination.provider
        && (!issue.versions || issue.versions.includes(majorMinor(combination.providerVersion)))
        && (!issue.databases || issue.databases.includes(combination.database))
        && issue.test.test(failure.test)
        && (!issue.message || issue.message.test(failure.message)));
}

function databaseVersion(key: string, version: string): string {
    if (key === 'MSSQL') {
        return SQL_SERVER_RELEASES[version.split('.')[0]] ?? version;
    }
    return version.replace(/\.0$/, '');
}

function Cell({combination}: { combination?: Combination }) {
    if (!combination) {
        return <td style={styles.cell} title="not tested">–</td>;
    }
    const broken = combination.failures + combination.errors;
    if (combination.outcome === 'ERROR' || combination.tests === 0) {
        return <td style={styles.cell} title="the run did not report any tests">❌</td>;
    }
    if (broken === 0) {
        const skipped = combination.skipped ? `, ${combination.skipped} skipped` : '';
        return <td style={styles.cell} title={`all ${combination.tests} tests passed${skipped}`}>✅</td>;
    }

    const issues = new Map<string, KnownIssue>();
    let unexplained = 0;
    for (const failure of combination.failed) {
        const issue = explain(combination, failure);
        if (issue) {
            issues.set(issue.label, issue);
        } else {
            unexplained++;
        }
    }
    const title = (broken >= combination.tests
            ? 'every test class failed while setting up'
            : `${combination.tests - broken} of ${combination.tests} tests passed`)
        + '\n' + combination.failed.map(failure => failure.test).join('\n');
    return (
        <td style={styles.cell} title={title}>
            {broken >= combination.tests ? '❌' : '⚠️'}
            {[...issues.values()].sort((a, b) => a.label.localeCompare(b.label)).map(issue =>
                <a key={issue.label} href={`#${issue.anchor}`} style={styles.label}>{issue.label}</a>)}
            {unexplained > 0 && <a href="#unexplained-failures" style={styles.label}>?</a>}
        </td>
    );
}

export default function CompatibilityMatrix() {
    const combinations = matrix.combinations as Combination[];

    // one row per provider version: each implements a single Jakarta Persistence version
    const rows = new Map<string, Combination[]>();
    for (const combination of combinations) {
        const key = `${combination.provider} ${combination.providerVersion}`;
        rows.set(key, [...(rows.get(key) ?? []), combination]);
    }
    const providers = Object.keys(PROVIDERS).filter(provider => combinations.some(c => c.provider === provider));
    const versionsOf = (provider: string) => [...rows.values()]
        .filter(row => row[0].provider === provider)
        .sort((a, b) => a[0].providerVersion.localeCompare(b[0].providerVersion, undefined, {numeric: true}));

    const unexplained = combinations.flatMap(combination => combination.failed
        .filter(failure => !explain(combination, failure))
        .map(failure => ({combination, failure})));

    return (
        <>
            <table style={styles.table}>
                <thead>
                <tr>
                    <th colSpan={2}>Provider</th>
                    {DATABASES.map(database => {
                        const tested = combinations.find(c => c.database === database.key);
                        return (
                            <th key={database.key} style={styles.cell}>
                                {database.name}
                                {tested && <span style={styles.databaseVersion}>{databaseVersion(database.key, tested.databaseVersion)}</span>}
                            </th>
                        );
                    })}
                </tr>
                </thead>
                <tbody>
                {providers.flatMap(provider => versionsOf(provider).map((row, index, all) => {
                    const first = row[0];
                    const java = JAVA_MINIMUM[`${provider} ${first.providerVersion.split('.')[0]}`];
                    return (
                        <tr key={`${provider} ${first.providerVersion}`}>
                            {index === 0 && <td rowSpan={all.length} style={styles.provider}>{PROVIDERS[provider]}</td>}
                            <td style={styles.version}>
                                {majorMinor(first.providerVersion)}
                                <span style={styles.details}>JPA {first.jpa.split('-')[0]}{java && ` · Java ${java}+`}</span>
                            </td>
                            {DATABASES.map(database =>
                                <Cell key={database.key} combination={row.find(c => c.database === database.key)}/>)}
                        </tr>
                    );
                }))}
                </tbody>
            </table>
            <p style={styles.caption}>
                ✅ all tests pass · ⚠️ some fail, the letters link to the explanation · ❌ the suite could not run ·
                hover a cell for the counts. Tested
                versions: {providers.map(provider => `${PROVIDERS[provider]} ${versionsOf(provider).map(row => row[0].providerVersion).join(', ')}`).join('; ')}.
                Generated {matrix.generatedAt} from commit <code>{matrix.commit}</code>, on
                Java {combinations[0]?.java}.
            </p>
            {unexplained.length > 0 && (
                <div id="unexplained-failures" style={styles.unexplained}>
                    <strong>Failures without an explanation yet</strong>
                    <ul>
                        {unexplained.map(({combination, failure}) => (
                            <li key={`${combination.provider}${combination.providerVersion}${combination.database}${failure.test}`}>
                                {PROVIDERS[combination.provider]} {majorMinor(combination.providerVersion)} on{' '}
                                {DATABASES.find(d => d.key === combination.database)?.name}: <code>{failure.test}</code>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </>
    );
}
