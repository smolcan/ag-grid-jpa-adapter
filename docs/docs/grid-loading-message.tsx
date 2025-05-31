import React, { useState, useEffect, ReactNode } from 'react';
import { useColorMode } from '@docusaurus/theme-common';

interface GridWrapperProps {
    children: ReactNode;
    serviceUrl?: string;
}

const GridLoadingMessage: React.FC<GridWrapperProps> = ({
                                                     children,
                                                     serviceUrl = 'https://ag-grid-jpa-adapter-docs-backend.onrender.com'
                                                 }) => {
    const [showSlowLoadingMessage, setShowSlowLoadingMessage] = useState<boolean>(false);
    const { colorMode } = useColorMode();

    useEffect(() => {
        let slowLoadingTimer: NodeJS.Timeout;
        let activeRequestCount = 0;

        // Monitor network requests to the backend service
        const originalFetch = window.fetch;

        window.fetch = async (...args) => {
            const [url] = args;

            // Check if this request is to our backend service
            if (typeof url === 'string' && url.includes(serviceUrl)) {
                activeRequestCount++;

                // Only start timer on first request
                if (activeRequestCount === 1) {
                    setShowSlowLoadingMessage(false);

                    // Show slow loading message after 5 seconds
                    slowLoadingTimer = setTimeout(() => {
                        setShowSlowLoadingMessage(true);
                    }, 5000);
                }
            }

            try {
                const response = await originalFetch(...args);

                // If this was a request to our service, decrement counter
                if (typeof url === 'string' && url.includes(serviceUrl)) {
                    activeRequestCount--;

                    // Only clear when all requests are done
                    if (activeRequestCount === 0) {
                        clearTimeout(slowLoadingTimer);
                        setShowSlowLoadingMessage(false);
                    }
                }

                return response;
            } catch (error) {
                // Clear message on error
                if (typeof url === 'string' && url.includes(serviceUrl)) {
                    activeRequestCount--;

                    // Only clear when all requests are done
                    if (activeRequestCount === 0) {
                        clearTimeout(slowLoadingTimer);
                        setShowSlowLoadingMessage(false);
                    }
                }
                throw error;
            }
        };

        // Cleanup function
        return () => {
            window.fetch = originalFetch;
            if (slowLoadingTimer) {
                clearTimeout(slowLoadingTimer);
            }
        };
    }, [serviceUrl]);

    return (
        <div>
            {/* Slow Loading Message - only shows after timeout */}
            {showSlowLoadingMessage && (
                <div style={{
                    backgroundColor: colorMode === 'dark' ? '#2d3748' : '#f7fafc',
                    border: `1px solid ${colorMode === 'dark' ? '#4a5568' : '#e2e8f0'}`,
                    color: colorMode === 'dark' ? '#e2e8f0' : '#2d3748',
                    padding: '1rem',
                    borderRadius: '8px',
                    fontSize: '0.875rem',
                    marginBottom: '1rem',
                    fontFamily: 'system-ui, -apple-system, sans-serif',
                    lineHeight: '1.4'
                }}>
                    <div style={{ fontWeight: '500', marginBottom: '0.25rem' }}>
                        ⏱️ Taking longer than expected?
                    </div>
                    <div style={{ opacity: 0.8 }}>
                        The free backend service might be starting up after inactivity. Please wait a moment.
                    </div>
                </div>
            )}

            {/* Render the wrapped component */}
            {children}
        </div>
    );
};

export default GridLoadingMessage;