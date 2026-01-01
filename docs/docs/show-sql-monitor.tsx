import React, { useState, useEffect, ReactNode } from 'react';
import { formatDialect, sql } from 'sql-formatter';
import { useColorMode } from '@docusaurus/theme-common';

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
                        <details key={i} style={{ marginBottom: '8px' }}>
                            <summary style={{ cursor: 'pointer', fontSize: '0.85rem' }}>
                                <strong>{group.timestamp}</strong> — {group.queries.length} queries
                            </summary>
                            <div style={{ marginTop: '8px' }}>
                                {group.queries.map((sql, j) => (
                                    <pre key={j} style={{
                                        fontSize: '0.75rem',
                                        padding: '12px',
                                        backgroundColor: colorMode === 'dark' ? '#0d1117' : '#fff',
                                        border: `1px solid ${colorMode === 'dark' ? '#30363d' : '#e1e4e8'}`,
                                        whiteSpace: 'pre-wrap',
                                        borderRadius: '6px'
                                    }}>
                                        {formatSql(sql)}
                                    </pre>
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