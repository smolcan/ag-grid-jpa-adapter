import React, { useState, useEffect, ReactNode } from 'react';
import { formatDialect, sql } from 'sql-formatter';
import { useColorMode } from '@docusaurus/theme-common';
import CodeBlock from '@theme/CodeBlock';

interface SqlGroup {
    timestamp: string;
    method: string;
    url: string;
    queries: string[];
    requestPayload: any;
    responsePayload: any;
}

interface GridWrapperProps {
    children: ReactNode;
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
        const previousFetch = window.fetch;

        window.fetch = async (...args) => {
            const [resource, config] = args;
            const requestUrl = typeof resource === 'string' ? resource : (resource as Request).url;
            const method = config?.method || 'GET';


            let requestPayload = null;
            if (config?.body) {
                try {
                    requestPayload = JSON.parse(config.body as string);
                } catch (e) {
                    requestPayload = config.body; // Ak to nie je JSON, uložíme raw
                }
            }

            const currentUrls = urlsRef.current ?? [];
            const isTargetRequest = currentUrls.some(url => requestUrl.includes(url));

            try {
                const response = await previousFetch(...args);

                if (isTargetRequest) {
                    try {
                        const cloned = response.clone();
                        const data = await cloned.json();

                        setSqlHistory(prev => [{
                            timestamp: new Date().toLocaleTimeString(),
                            method: method,
                            url: requestUrl.split('/').pop() || requestUrl,
                            queries: data.sql || [],
                            requestPayload: requestPayload,
                            responsePayload: data
                        }, ...prev]);
                    } catch (e) {
                    }
                }

                return response;
            } catch (error) {
                throw error;
            }
        };

        return () => {
            window.fetch = previousFetch;
        };
    }, []);

    const formatSql = (sqlString: string) => {
        try {
            return formatDialect(sqlString, { dialect: sql });
        } catch (e) {
            return sqlString;
        }
    };

    const renderJson = (data: any) => {
        return JSON.stringify(data, null, 2);
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
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.7rem', fontWeight: 'bold', color: '#888' }}>
                            SQL & API MONITOR
                        </span>
                        <button
                            onClick={() => setSqlHistory([])}
                            style={{
                                fontSize: '0.6rem',
                                cursor: 'pointer',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                border: '1px solid #ccc'
                            }}
                        >
                            Clear Logs
                        </button>
                    </div>

                    {sqlHistory.map((group, i) => (
                        <details key={i} style={{ marginBottom: '12px', borderBottom: '1px solid #eee', paddingBottom: '8px' }}>
                            <summary style={{ cursor: 'pointer', fontSize: '0.85rem' }}>
                                <code style={{ marginRight: '8px', color: '#2ecc71' }}>{group.method}</code>
                                <strong>{group.timestamp}</strong> — {group.url} ({group.queries.length} SQLs)
                            </summary>

                            <div style={{ marginTop: '10px', marginLeft: '15px' }}>
                                {/* Request Section */}
                                {group.requestPayload && (
                                    <details style={{ marginBottom: '5px' }}>
                                        <summary style={{ fontSize: '0.75rem', color: '#e67e22', cursor: 'pointer' }}>Request Payload</summary>
                                        <CodeBlock language="json">{renderJson(group.requestPayload)}</CodeBlock>
                                    </details>
                                )}

                                {/* SQL Section */}
                                {group.queries.length > 0 && (
                                    <details>
                                        <summary style={{ fontSize: '0.75rem', color: '#3498db', cursor: 'pointer' }}>SQL Queries</summary>
                                        <div style={{ marginTop: '5px' }}>
                                            {group.queries.map((sqlText, j) => (
                                                <CodeBlock key={j} language="sql">
                                                    {formatSql(sqlText)}
                                                </CodeBlock>
                                            ))}
                                        </div>
                                    </details>
                                )}

                                {/* Response Section */}
                                <details style={{ marginTop: '5px' }}>
                                    <summary style={{ fontSize: '0.75rem', color: '#9b59b6', cursor: 'pointer' }}>Full Response</summary>
                                    <CodeBlock language="json">{renderJson(group.responsePayload)}</CodeBlock>
                                </details>
                            </div>
                        </details>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ShowSqlMonitor;