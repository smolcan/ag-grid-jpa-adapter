import React, { useState, useEffect, ReactNode } from 'react';
import { formatDialect, sql } from 'sql-formatter';
import { useColorMode } from '@docusaurus/theme-common';
import CodeBlock from '@theme/CodeBlock';

interface SqlGroup {
    timestamp: string;
    queries: string[];
}

interface GridWrapperProps {
    children: ReactNode;
    // Changed from string to string[]
    serviceUrls: string[];
}

const ShowSqlMonitor: React.FC<GridWrapperProps> = ({ children, serviceUrls }) => {
    const [sqlHistory, setSqlHistory] = useState<SqlGroup[]>([]);
    const { colorMode } = useColorMode();
    const urlsRef = React.useRef(serviceUrls);
    useEffect(() => {
        urlsRef.current = serviceUrls;
    }, [serviceUrls]);

    useEffect(() => {
        let activeRequestCount = 0;

        // Save reference to the fetch function currently in place
        const previousFetch = window.fetch;

        window.fetch = async (...args) => {
            const [resource] = args;
            const requestUrl = typeof resource === 'string' ? resource : (resource as Request).url;

            // Check if this request matches ANY of our target URLs
            const currentUrls = urlsRef.current ?? [];
            const isTargetRequest = currentUrls.some(url => requestUrl.includes(url));

            try {
                const response = await previousFetch(...args);

                if (isTargetRequest) {
                    activeRequestCount--;

                    // Logic to extract SQL
                    try {
                        const cloned = response.clone();
                        const data = await cloned.json();
                        if (data && data.sql) {
                            setSqlHistory(prev => [{
                                timestamp: new Date().toLocaleTimeString(),
                                queries: data.sql
                            }, ...prev]);
                        }
                    } catch (e) { /* Not JSON */ }
                }

                return response;
            } catch (error) {
                throw error;
            }
        };

        return () => {
            window.fetch = previousFetch;
        };
    }, [serviceUrls]); // Re-bind if the list of URLs changes

    const formatSql = (sqlString: string) => {
        try {
            return formatDialect(sqlString, {dialect: sql});
        } catch (e) {
            return sqlString; 
        }
    };

    return (
        <div style={{ marginBottom: '2rem' }}>

            {children}

            {sqlHistory.length > 0 && (
                <div style={{
                    marginTop: '1rem',
                    padding: '1rem',
                    backgroundColor: colorMode === 'dark' ? '#161b22' : '#f8f9fa',
                    border: `1px solid ${colorMode === 'dark' ? '#30363d' : '#d1d5da'}`,
                    borderRadius: '8px'
                }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                        <span style={{ fontSize: '0.7rem', fontWeight: 'bold' }}>
                            SQL MONITOR ({serviceUrls.length} Endpoints)
                        </span>
                        <button onClick={() => setSqlHistory([])} style={{ fontSize: '0.6rem' }}>Clear</button>
                    </div>

                    {sqlHistory.map((group, i) => (
                        <details key={i} style={{ marginBottom: '8px' }} open={i === 0}>
                            <summary style={{ cursor: 'pointer', fontSize: '0.85rem' }}>
                                <strong>{group.timestamp}</strong> — {group.queries.length} queries
                            </summary>
                            <div style={{ marginTop: '8px' }}>
                                {group.queries.map((sqlText, j) => (
                                    <div key={j} style={{ marginBottom: '10px' }}>
                                        <CodeBlock language="sql">
                                            {formatSql(sqlText)}
                                        </CodeBlock>
                                    </div>
                                ))}
                            </div>
                        </details>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ShowSqlMonitor;